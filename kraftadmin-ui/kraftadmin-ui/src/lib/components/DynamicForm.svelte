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
  <div class="form-container">
    <div class="text-danger font-bold mb-2">⚠ Form Error</div>
    <p class="text-text-muted text-sm">{formError.message}</p>
    {#if formError.raw}
      <pre class="bg-bg-main text-danger p-4 rounded-lg text-[11px] mt-4 overflow-x-auto">{formError.raw}</pre>
    {/if}
    <button type="button" on:click={() => formError = null} class="btn-secondary mt-4 text-xs">
      Try Anyway
    </button>
  </div>
{:else}
  <div class="form-container !p-0">
    <div class="grid grid-cols-1 md:grid-cols-2 p-8 gap-6">
      {#each fields as col (col.name)}
        {@const error = externalErrors[col.name]}
        <div class="flex flex-col gap-1.5 {['TEXTAREA', 'JSON', 'OBJECT', 'ARRAY', 'WYSIWYG', 'RELATION', 'MULTI_RELATION', 'VIDEO', 'IMAGE', 'FILE', 'AUDIO', 'DOCUMENT'].includes(col.type) ? 'md:col-span-2' : ''}">
          
          <label class="field-label">
            {col.label}
            {#if col.required}<span class="req-star">*</span>{/if}
          </label>

          {#if col.type === 'CHECKBOX'}
            <button type="button" on:click={() => formData[col.name] = !formData[col.name]}
              class="w-11 h-6 rounded-full relative transition-colors {formData[col.name] ? 'bg-brand-primary' : 'bg-border-subtle'}">
              <div class="absolute top-1 left-1 bg-bg-surface w-4 h-4 rounded-full transition-transform {formData[col.name] ? 'translate-x-5' : ''}"></div>
            </button>

          {:else if col.type === 'RADIO'}
            <div class="flex flex-wrap gap-4 py-2">
              {#each col.selectOptions || [] as opt}
                <label class="flex items-center gap-2 cursor-pointer text-text-main">
                  <input type="radio" bind:group={formData[col.name]} value={opt.value} />
                  <span class="text-sm">{opt.label}</span>
                </label>
              {/each}
            </div>

          {:else if col.type === 'MULTI_SELECT'}
  <select multiple bind:value={formData[col.name]} class="input-base {error ? 'input-error' : ''}">
    {#each col.selectOptions || [] as opt}
      <option value={opt.value}>{opt.label}</option>
    {/each}
  </select>

{:else if col.type === 'SELECT'}
  <select bind:value={formData[col.name]} class="input-base {error ? 'input-error' : ''}">
    <option value={null}>Select {col.label}...</option>
    {#each col.selectOptions || [] as opt}
      <option value={opt.value}>{opt.label}</option>
    {/each}
  </select>

          {:else if col.type === 'RELATION'}
            <input type="text" class="input-base {error ? 'input-error' : ''}" placeholder="Search..." bind:value={searchTerms[col.name]} on:input={(e) => handleSearchInput(e, col)} />

          {:else if col.type === 'MULTI_RELATION'}
            <div class="input-base min-h-[42px] flex flex-wrap gap-2 {error ? 'input-error' : ''}">
              {#each (formData[col.name] || []) as itemId}
                <div class="chip">
                  {searchTerms[`${col.name}_${itemId}`] || itemId}
                  <button type="button" on:click={() => removeTag(col.name, itemId)}>&times;</button>
                </div>
              {/each}
              <input type="text" class="bg-transparent border-none outline-none flex-1 min-w-[120px]" placeholder="Search..." bind:value={searchTerms[col.name]} on:input={(e) => handleSearchInput(e, col)} />
            </div>

          {:else if col.type === 'COLOR'}
            <div class="flex gap-2">
              <input type="color" bind:value={formData[col.name]} class="h-10 w-10 rounded-lg border border-border-subtle cursor-pointer" />
              <input type="text" bind:value={formData[col.name]} class="input-base font-mono {error ? 'input-error' : ''}" />
            </div>

          {:else if ['NUMBER', 'DATE', 'DATETIME', 'TIME', 'RANGE'].includes(col.type)}
            <input type={col.type === 'RANGE' ? 'range' : col.type.toLowerCase()} bind:value={formData[col.name]} class="input-base {error ? 'input-error' : ''}" />

          {:else if ['IMAGE', 'VIDEO', 'FILE', 'AUDIO', 'DOCUMENT'].includes(col.type)}
            <FileUploader {col} value={formData[col.name]} uploading={uploading === col.name} on:change={(e) => handleFileChange(e.detail.files, col.name)} on:clear={(e) => handleFileRemoval(col.name, e.detail.index)} />

          {:else if col.type === 'WYSIWYG'}
            <WYSIWYG value={formData[col.name]} config={col.wysiwygConfig} onChange={(html) => formData[col.name] = html} />

          {:else if col.type === 'OBJECT'}
            <EmbeddedObjectInput label={col.label} subColumns={col.subColumns} bind:value={formData[col.name]} />

          {:else if ['JSON', 'TEXTAREA', 'ARRAY'].includes(col.type)}
            <textarea bind:value={formData[col.name]} rows={6} class="input-base !h-auto {error ? 'input-error' : ''}"></textarea>

          {:else}
            <input type="text" bind:value={formData[col.name]} class="input-base {error ? 'input-error' : ''}" />
          {/if}

          {#if error}
            <div class="text-danger text-[10px] font-bold uppercase">{error[0]}</div>
          {/if}
        </div>
      {/each}
    </div>

    <div class="form-footer px-8">
      <button type="button" on:click={handleCancel} class="btn-secondary">Cancel</button>
      <button type="button" on:click={triggerSubmit} class="btn-primary">
        {submitLabel} {resourceName}
      </button>
    </div>
  </div>
{/if}