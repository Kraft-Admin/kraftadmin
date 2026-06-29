<script lang="ts">
  import EmbeddedObjectInput from '../components/EmbeddedObjectInput.svelte';
  import FileUploader from './FileUploader.svelte';
  import WYSIWYG from './WYSIWYG.svelte';
 
  export let columns: any[] = [];
  export let onSubmit: (data: any) => void;
  export let resourceName: string = "";
  export let submitLabel: string = "Create ";
  export let initialData: any = {};
  export let externalErrors: Record<string, string[]> = {};

  let formData: any = {};
  let searchResults: Record<string, any[]> = {};
  let searchTerms: Record<string, string> = {};
  let debounceTimer: any;
  let activeSearchCol: string | null = null;
  let lastLoadedId: string | null = null;
  let formError: { message: string; raw?: any } | null = null;

  let fields: any[] = [];
  let lastColumnsJson = '';
  $: {
    try {
      const json = JSON.stringify(columns);
      if (json !== lastColumnsJson) {
        lastColumnsJson = json;
        fields = columns.filter(c => c.visible && !['id', 'createdAt', 'updatedAt', 'deletedAt'].includes(c.name));
      }
    } catch (e: any) {
      formError = { message: 'Failed to process column definitions', raw: e?.message };
    }
  }

  function arrayToDatetimeLocal(arr: any[]): string {
    const [y, m, d, hh = 0, mm = 0] = arr;
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}T${String(hh).padStart(2, '0')}:${String(mm).padStart(2, '0')}`;
  }

  function isSafeValue(type: string, val: any): boolean {
    if (val === null || val === undefined || val === '') return true;
    switch (type) {
      case 'TEXT': case 'TEXTAREA': case 'WYSIWYG': case 'JSON': case 'SELECT': case 'EMAIL': case 'URL': case 'PASSWORD': case 'TEL':
        return typeof val === 'string';
      case 'NUMBER': case 'RANGE':
        return typeof val === 'number';
      case 'CHECKBOX':
        return typeof val === 'boolean';
      case 'DATETIME': case 'DATE': case 'TIME':
        return typeof val === 'string';
      case 'ARRAY': case 'MULTI_SELECT': case 'MULTI_RELATION':
        return Array.isArray(val);
      case 'RELATION':
        return typeof val === 'string' || val === null;
      case 'OBJECT':
        return typeof val === 'object' && !Array.isArray(val);
      case 'IMAGE': case 'VIDEO': case 'FILE':
        return typeof val === 'string';
      default:
        return true;
    }
  }

  $: if(initialData && initialData.id){
    submitLabel = "Update ";
  }

  $: if (columns && columns.length > 0) {
    try {
      const currentId = initialData?.id || 'new';
      if (currentId !== lastLoadedId) {
        const newData: any = {};
        const values = initialData.values || {};
        columns.forEach(col => {
          try {
            const existingValue = values[col.name] ?? null;
            if (col.type === 'OBJECT') {
              newData[col.name] = {};
              const dataSource = existingValue?.data ?? existingValue ?? {};
              col.subColumns?.forEach((sub: any) => {
                const rawSubValue = dataSource[sub.name] ?? null;
                newData[col.name][sub.name] = (Array.isArray(rawSubValue) && sub.type === 'DATETIME') ? arrayToDatetimeLocal(rawSubValue) : (rawSubValue ?? sub.defaultValue ?? "");
              });
            } else if (col.type === 'DATETIME') {
              newData[col.name] = Array.isArray(existingValue) ? arrayToDatetimeLocal(existingValue) : (existingValue ?? "");
            } else if (col.type === 'DATE') {
              newData[col.name] = Array.isArray(existingValue) ? `${existingValue[0]}-${String(existingValue[1]).padStart(2, '0')}-${String(existingValue[2]).padStart(2, '0')}` : (existingValue ?? "");
            } else if (col.type === 'TIME') {
              newData[col.name] = (typeof existingValue === 'string') ? (existingValue.includes('T') ? existingValue.split('T')[1].substring(0, 5) : existingValue.substring(0, 5)) : "";
            } else if (col.type === 'RELATION') {
              newData[col.name] = (existingValue && typeof existingValue === 'object') ? existingValue.id : (existingValue ?? null);
              if (existingValue?.displayField && !searchTerms[col.name]) searchTerms[col.name] = existingValue.displayField;
            } else if (col.type === 'MULTI_RELATION') {
              newData[col.name] = Array.isArray(existingValue) ? existingValue.map(item => (typeof item === 'object' ? item.id : item)) : [];
              if (Array.isArray(existingValue)) existingValue.forEach(item => { if (item?.id) searchTerms[`${col.name}_${item.id}`] = item.displayField || item.name || item.id; });
            } else if (col.type === 'ARRAY' || col.type === 'MULTI_SELECT') {
              newData[col.name] = Array.isArray(existingValue) ? existingValue : (typeof existingValue === 'string' ? existingValue.split(',').map(s => s.trim()).filter(Boolean) : []);
            } else if (col.type === 'NUMBER' || col.type === 'RANGE') {
              newData[col.name] = typeof existingValue === 'number' ? existingValue : (existingValue ?? 0);
            } else if (col.type === 'CHECKBOX') {
              newData[col.name] = typeof existingValue === 'boolean' ? existingValue : false;
            } else {
              newData[col.name] = (existingValue !== null && typeof existingValue === 'object') ? (Array.isArray(existingValue) ? existingValue.join(', ') : (existingValue.displayField ?? existingValue.id ?? JSON.stringify(existingValue))) : (existingValue ?? col.defaultValue ?? "");
            }
          } catch (e) { newData[col.name] = null; }
        });
        formData = newData;
        lastLoadedId = currentId;
      }
    } catch (e) { formError = { message: 'Failed to initialize form', raw: e }; }
  }

  async function fetchLookups(lookup: any, colName: string, query: string) {
    if (!lookup?.targetEntity || !query) { searchResults[colName] = []; return; }
    try {
      const res = await fetch(`/admin/api/resources/${lookup.targetEntity}/lookup/${lookup.searchField || 'name'}?search=${encodeURIComponent(query)}`);
      if (res.ok) searchResults[colName] = await res.json();
    } catch (e) { console.error("Lookup failed", e); }
  }

  function handleSearchInput(e: Event, col: any) {
    const query = (e.target as HTMLInputElement).value;
    searchTerms[col.name] = query;
    activeSearchCol = col.name;
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => fetchLookups(col.lookup, col.name, query), 300);
  }

  function selectSingle(col: any, option: any) {
    formData[col.name] = option.id;
    searchTerms[col.name] = option.displayField || option.name;
    activeSearchCol = null;
  }

  function selectMulti(col: any, option: any) {
    const current = formData[col.name] || [];
    if (!current.includes(option.id)) {
      formData[col.name] = [...current, option.id];
      searchTerms[`${col.name}_${option.id}`] = option.displayField || option.name;
    }
    searchTerms[col.name] = "";
    searchResults[col.name] = [];
  }

  function removeTag(colName: string, id: any) {
    formData[colName] = formData[colName].filter((existingId: any) => existingId !== id);
  }

  let uploading: string | null = null;

  async function handleFileChange(files: FileList, fieldName: string) {
    if (!files || files.length === 0) return;
    const col = columns.find(c => c.name === fieldName);
    const isMultiple = col?.fileConfig?.multiple ?? false;
    uploading = fieldName;
    const fd = new FormData();
    if (isMultiple) { Array.from(files).forEach(f => fd.append('files', f)); } 
    else { fd.append('file', files[0]); if (formData[fieldName]) fd.append('oldUrl', formData[fieldName]); }
    try {
      const response = await fetch('/admin/api/uploads', { method: 'POST', body: fd });
      if (!response.ok) throw new Error('Upload failed');
      const result = await response.json();
      formData[fieldName] = isMultiple ? [...(formData[fieldName] || []), ...result.urls] : result.url;
    } finally { uploading = null; }
  }
  
  async function deleteFileFromServer(url: string) {
    try { await fetch('/admin/api/uploads', { method: 'DELETE', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ url }) }); } catch (err) { console.error(err); }
  }

  async function handleFileRemoval(fieldName: string, index: number | null) {
    const val = formData[fieldName];
    if (Array.isArray(val)) {
      const url = val[index!];
      if (url) await deleteFileFromServer(url);
      formData[fieldName] = val.filter((_, i) => i !== index);
    } else {
      if (val) await deleteFileFromServer(val);
      formData[fieldName] = null;
    }
  }

  async function handleCancel() {
    for (const col of columns) {
      if (['IMAGE', 'VIDEO', 'FILE'].includes(col.type) && formData[col.name]) {
        const val = formData[col.name];
        if (Array.isArray(val)) { for (const url of val) await deleteFileFromServer(url); } 
        else { await deleteFileFromServer(val); }
      }
    }
    window.history.back();
  }

  const triggerSubmit = () => onSubmit(formData);
</script>

{#if formError}
  <div class="form-container" style="padding: 1.5rem;">
    <div style="color: #ef4444; font-weight: bold; margin-bottom: 0.5rem;">⚠ Form Error</div>
    <p style="color: #a1a1aa; font-size: 0.875rem;">{formError.message}</p>
    {#if formError.raw}
      <pre style="background: #000; color: #ef4444; padding: 1rem; border-radius: 0.5rem; font-size: 11px; margin-top: 1rem; overflow-x: auto;">{formError.raw}</pre>
    {/if}
    <button type="button" on:click={() => formError = null}
      style="margin-top: 1rem; padding: 0.5rem 1rem; background: #27272a; color: #e4e4e7; border: none; border-radius: 0.5rem; cursor: pointer; font-size: 12px;">
      Try Anyway
    </button>
  </div>
{:else}

  <div class="form-container">
    <div class="form-grid">
      {#each fields as col (col.name)}
        {@const val = formData[col.name]}
        {@const safe = isSafeValue(col.type, val)}

        <div class="field-group {['TEXTAREA', 'JSON', 'OBJECT', 'ARRAY', 'WYSIWYG', 'RELATION', 'MULTI_RELATION', 'VIDEO'].includes(col.type) ? 'span-2' : ''}">
          <label for={col.name} class="field-label">
            {col.label}
            {#if col.required}<span class="req-star">*</span>{/if}
          </label>

          {#if !safe}
            <div style="padding: 0.5rem 0.75rem; border: 1px dashed #ef4444; border-radius: 0.5rem; font-size: 10px; color: #ef4444; font-family: monospace;">
              ⚠ type mismatch: expected {col.type}, got {Array.isArray(val) ? 'array' : typeof val} — {JSON.stringify(val)?.substring(0, 80)}
            </div>

          {:else if col.type === 'CHECKBOX'}
            <div class="toggle-wrapper">
              <button type="button" on:click={() => formData[col.name] = !formData[col.name]}
                class="toggle-btn" class:active={formData[col.name]}>
                <div class="toggle-dot" class:dot-active={formData[col.name]}></div>
              </button>
            </div>

          {:else if col.type === 'RADIO'}
            <div class="flex flex-wrap gap-4 py-2">
              {#each col.selectOptions || [] as opt}
                <label class="flex items-center gap-2 cursor-pointer">
                  <input type="radio" bind:group={formData[col.name]} value={opt.value} class="accent-brand" />
                  <span class="text-sm text-zinc-300">{opt.label}</span>
                </label>
              {/each}
            </div>

          {:else if col.type === 'SELECT'}
            <select bind:value={formData[col.name]} class="input-base" class:input-error={externalErrors[col.name]} required={col.required}>
              <option value={null}>{col.placeholder || `Select ${col.label}...`}</option>
              {#each col.selectOptions || [] as opt}
                <option value={opt.value}>{opt.label}</option>
              {/each}
            </select>

          {:else if col.type === 'MULTI_SELECT'}
            <select multiple bind:value={formData[col.name]} class="input-base min-h-[100px]" class:input-error={externalErrors[col.name]} required={col.required}>
              {#each col.selectOptions || [] as opt}
                <option value={opt.value}>{opt.label}</option>
              {/each}
            </select>

          {:else if col.type === 'RELATION'}
            <div class="relation-wrapper">
              <input type="text" class="input-base" class:input-error={externalErrors[col.name]} placeholder="Search to link..."
                bind:value={searchTerms[col.name]}
                on:focus={() => activeSearchCol = col.name}
                on:input={(e) => handleSearchInput(e, col)} />
              {#if activeSearchCol === col.name && searchResults[col.name]?.length > 0}
                <div class="lookup-dropdown">
                  {#each searchResults[col.name] as opt}
                    <button type="button" class="lookup-item" on:click={() => selectSingle(col, opt)}>
                      {opt.displayField || opt.name}
                    </button>
                  {/each}
                </div>
              {/if}
            </div>

          {:else if col.type === 'MULTI_RELATION'}
            <div class="relation-wrapper">
              <div class="input-base flex flex-wrap gap-2 items-center min-h-[42px] focus-within:border-blue-500" class:input-error={externalErrors[col.name]}>
                {#each (formData[col.name] || []) as itemId}
                  <div class="chip">
                    <span class="text-[11px]">{searchTerms[`${col.name}_${itemId}`] || itemId}</span>
                    <button type="button" class="ml-1 opacity-60 hover:opacity-100"
                      on:click={() => removeTag(col.name, itemId)}>&times;</button>
                  </div>
                {/each}
                <input type="text"
                  class="bg-transparent border-none outline-none flex-1 min-w-[120px] text-sm text-white"
                  placeholder={(formData[col.name]?.length > 0) ? "" : "Search to add..."}
                  bind:value={searchTerms[col.name]}
                  on:focus={() => activeSearchCol = col.name}
                  on:input={(e) => handleSearchInput(e, col)} />
              </div>
              {#if activeSearchCol === col.name && searchResults[col.name]?.length > 0}
                <div class="lookup-dropdown">
                  {#each searchResults[col.name] as opt}
                    <button type="button" class="lookup-item flex justify-between" on:click={() => selectMulti(col, opt)}>
                      <span>{opt.displayField || opt.name}</span>
                      {#if formData[col.name]?.includes(opt.id)}<span class="text-brand-primary">✓</span>{/if}
                    </button>
                  {/each}
                </div>
              {/if}
            </div>

          {:else if col.type === 'DATE'}
            <input type="date" bind:value={formData[col.name]} class="input-base" class:input-error={externalErrors[col.name]} />

          {:else if col.type === 'DATETIME'}
            <input type="datetime-local" bind:value={formData[col.name]} class="input-base" class:input-error={externalErrors[col.name]} />

          {:else if col.type === 'TIME'}
            <input type="time" bind:value={formData[col.name]} class="input-base" class:input-error={externalErrors[col.name]} />

          {:else if col.type === 'NUMBER' || col.type === 'RANGE'}
            <div class="flex flex-col gap-2">
              <input type={col.type === 'RANGE' ? 'range' : 'number'} step="any"
                bind:value={formData[col.name]}
                class={col.type === 'RANGE' ? 'range-input' : 'input-base'}
                class:input-error={externalErrors[col.name]} />
              {#if col.type === 'RANGE'}<span class="text-xs text-brand-primary font-mono">{formData[col.name] || 0}</span>{/if}
            </div>

        {:else if col.type === 'COLOR'}
            <div class="flex gap-2">
              <input type="color" bind:value={formData[col.name]} class="color-swatch" />
              <input type="text" bind:value={formData[col.name]} class="input-base font-mono" class:input-error={externalErrors[col.name]} placeholder="#000000" />
            </div>

          <!-- {:else if ['IMAGE', 'VIDEO', 'FILE'].includes(col.type)}
            <FileUploader
              {col} 
              value={formData[col.name]} 
              uploading={uploading === col.name}
              on:change={(e) => handleFileChange(e.detail.files, col.name)}
              on:clear={(e) => clearFile(col.name, e.detail.index)}
            /> -->

            {:else if ['IMAGE', 'VIDEO', 'FILE'].includes(col.type)}
            <FileUploader
              {col} 
              value={formData[col.name]} 
              uploading={uploading === col.name}
              on:change={(e) => handleFileChange(e.detail.files, col.name)}
              on:clear={(e) => handleFileRemoval(col.name, e.detail.index)}
            />

          
          
            {:else if col.type === 'ARRAY'}
            <input type="text" placeholder="e.g. Kotlin, Java, Go" class="input-base" class:input-error={externalErrors[col.name]}
              value={Array.isArray(formData[col.name]) ? formData[col.name].join(', ') : ''}
              on:input={(e) => formData[col.name] = e.currentTarget.value.split(',').map((s: string) => s.trim()).filter(Boolean)} /> 

          {:else if col.type === 'JSON' || col.type === 'TEXTAREA'}
            <textarea bind:value={formData[col.name]} rows={6} class="input-base font-mono text-sm"
              class:input-error={externalErrors[col.name]}
              placeholder={col.type === 'JSON' ? '{"key": "value"}' : 'Write here...'}></textarea>

          {:else if col.type === 'WYSIWYG'}
            <WYSIWYG 
              value={formData[col.name]} 
              config={col.wysiwygConfig} 
              onChange={(html) => formData[col.name] = html} 
            />

          {:else if col.type === 'OBJECT'}
            <EmbeddedObjectInput
              label={col.label}
              subColumns={col.subColumns}
              bind:value={formData[col.name]}
            />

          {:else}
            <input
              type={['EMAIL', 'PASSWORD', 'TEL', 'URL'].includes(col.type) ? col.type.toLowerCase() : 'text'}
              placeholder={col.placeholder || ''}
              bind:value={formData[col.name]}
              class="input-base"
              class:input-error={externalErrors[col.name]}
            />
          {/if}

          {#if externalErrors[col.name]}
            <div class="mt-1.5 flex flex-col gap-1">
              {#each externalErrors[col.name] as error}
                <div class="flex items-center gap-1 text-red-500 animate-in">
                  <svg xmlns="http://www.w3.org/2000/svg" class="w-3 h-3" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
                  </svg>
                  <span class="text-[10px] font-bold uppercase tracking-tight">{error}</span>
                </div>
              {/each}
            </div>
          {/if}
        </div>
      {/each}
    </div>

    <div class="form-footer">
      <button type="button" on:click={handleCancel} class="btn-cancel">Cancel</button>
      <button type="button" on:click={triggerSubmit} class="btn-submit">{submitLabel} {resourceName}</button>
    </div>
  </div>
{/if}

<style>
  .form-container { background-color: var(--bg-surface, #121214); border: 1px solid var(--border-subtle, #27272a); border-radius: 0.75rem; }
  .form-grid { padding: 2rem; display: grid; grid-template-columns: 1fr; gap: 1.5rem; }
  @media (min-width: 768px) { .form-grid { grid-template-columns: repeat(2, 1fr); } .span-2 { grid-column: span 2; } }

  .field-group { display: flex; flex-direction: column; gap: 0.375rem; }
  .field-label { font-size: 0.75rem; font-weight: 700; color: #71717a; text-transform: uppercase; letter-spacing: 0.05em; }
  .req-star { color: #ef4444; }

  .input-base { width: 100%; padding: 0.75rem; border-radius: 0.5rem; border: 1px solid #27272a; background-color: #09090b; color: #fafafa; outline: none; transition: all 0.2s; }
  .input-base:focus { border-color: #3b82f6; box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.2); }

  .relation-wrapper { position: relative; }
  .lookup-dropdown { position: absolute; top: 100%; left: 0; right: 0; z-index: 50; background: #18181b; border: 1px solid #27272a; border-radius: 0.5rem; margin-top: 4px; max-height: 200px; overflow-y: auto; }
  .lookup-item { width: 100%; text-align: left; padding: 0.75rem; background: transparent; border: none; border-bottom: 1px solid #27272a; color: #e4e4e7; font-size: 0.875rem; cursor: pointer; }
  .lookup-item:hover { background: #27272a; color: #3b82f6; }

  .toggle-btn { width: 2.75rem; height: 1.5rem; border-radius: 99px; position: relative; background-color: #3f3f46; border: none; cursor: pointer; transition: background 0.3s; }
  .toggle-btn.active { background-color: #3b82f6; }
  .toggle-dot { position: absolute; top: 0.25rem; left: 0.25rem; background-color: white; width: 1rem; height: 1rem; border-radius: 50%; transition: transform 0.2s; }
  .dot-active { transform: translateX(1.25rem); }

  .color-swatch { width: 3rem; height: 3rem; border: 1px solid #27272a; border-radius: 0.5rem; background: none; cursor: pointer; padding: 0; }
  .range-input { width: 100%; accent-color: #3b82f6; cursor: pointer; }

  .form-footer { padding: 1.5rem 2rem; background-color: #09090b; border-top: 1px solid #27272a; display: flex; justify-content: flex-end; gap: 1rem; }

  .btn-submit { padding: 0.625rem 1.5rem; background-color: #3b82f6; color: white; font-weight: 600; border-radius: 0.5rem; border: none; cursor: pointer; }
  .btn-cancel { color: #a1a1aa; background: none; border: none; cursor: pointer; font-size: 0.875rem; }

  .flex { display: flex; }
  .items-center { align-items: center; }
  .gap-2 { gap: 0.5rem; }
  .gap-4 { gap: 1rem; }
  .font-mono { font-family: ui-monospace, monospace; }

  .chip { display: inline-flex; align-items: center; background-color: #27272a; color: #3b82f6; padding: 0.25rem 0.625rem; border-radius: 9999px; border: 1px solid #3b82f6; font-weight: 500; }
  .ml-1 { margin-left: 0.25rem; }
  .text-brand-primary { color: #3b82f6; }

  .input-error { border-color: #ef4444 !important; background-color: #1a1010 !important; }
  .input-base.input-error { border-color: #ef4444 !important; background-color: rgba(239, 68, 68, 0.05) !important; }

  @keyframes fadeIn { from { opacity: 0; transform: translateY(-2px); } to { opacity: 1; transform: translateY(0); } }
  .animate-in { animation: fadeIn 0.2s ease-out forwards; }
</style>