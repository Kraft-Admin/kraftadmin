<script lang="ts">
  import { createEventDispatcher } from 'svelte';
  import FieldRenderer from '../components/inputs/FieldRenderer.svelte';

  export let columns: any[] = [];
  export let onSubmit: (data: any) => void;
  export let resourceName: string = "";
  export let submitLabel: string = "Save";
  export let initialData: any = {};
  export let externalErrors: Record<string, string[]> = {};

  let formData: any = {};
  let lastLoadedId: string | null = null;
  let fields: any[] = [];
  let lastColumnsJson = '';
  

  // Track which URLs were uploaded during THIS session
  // so cancel knows what to clean up
  let sessionUploadedUrls = new Set<string>();

  // Track original file URLs from initialData so we never
  // delete files that existed before this edit session started
  let originalFileUrls = new Set<string>();

  $: {
    const json = JSON.stringify(columns);
    if (json !== lastColumnsJson) {
      lastColumnsJson = json;
      fields = columns.filter(
        c => c.visible && !['id', 'createdAt', 'updatedAt', 'deletedAt'].includes(c.name)
      );
    }
  }

  $: if (columns?.length > 0) {
    const currentId = initialData?.id || 'new';
    if (currentId !== lastLoadedId) {
      formData = buildFormData(columns, initialData);
      lastLoadedId = currentId;

      // Snapshot original file URLs so cancel doesn't delete pre-existing files
      originalFileUrls = new Set(collectFileUrls(formData));
      sessionUploadedUrls = new Set(); // reset on new record load
    }
  }

  $: if (initialData?.id) submitLabel = "Update";

  // ─── File URL collection helpers ─────────────────────────────────────────

  function collectFileUrls(data: Record<string, any>): string[] {
    const urls: string[] = [];
    columns.forEach(col => {
      if (['IMAGE', 'VIDEO', 'FILE', 'AUDIO', 'DOCUMENT'].includes(col.type)) {
        const val = data[col.name];
         console.log("FIELD", col.name);
        console.log("VALUE", val);
        console.log("MULTIPLE", col.fileOptions?.multiple);
        if (typeof val === 'string' && val) urls.push(val);
        if (Array.isArray(val)) val.filter(Boolean).forEach(v => urls.push(v));
      } else if (col.type === 'OBJECT' && col.subColumns) {
        const obj = data[col.name] ?? {};
        col.subColumns.forEach((sub: any) => {
          if (['IMAGE', 'VIDEO', 'FILE', 'AUDIO', 'DOCUMENT'].includes(sub.type)) {
            const val = obj[sub.name];
            if (typeof val === 'string' && val) urls.push(val);
          }
        });
      }
    });
    return urls.filter(Boolean);
  }

  async function deleteUrl(url: string) {
    try {
      await fetch('/admin/api/uploads', {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url })
      });
    } catch (e) {
      console.error('[DynamicForm] Failed to delete uploaded file:', url, e);
    }
  }

  // ─── Cancel — delete session-uploaded files that weren't originally there ──

  async function handleCancel() {
    // Only delete URLs that were uploaded in this session AND
    // weren't part of the original data (i.e. new uploads only)
    const toDelete = [...sessionUploadedUrls].filter(url => !originalFileUrls.has(url));

    if (toDelete.length > 0) {
      await Promise.all(toDelete.map(deleteUrl));
    }

    window.history.back();
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────

  function arrayToDatetimeLocal(arr: any[]): string {
    const [y, m, d, hh = 0, mm = 0] = arr;
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}T${String(hh).padStart(2, '0')}:${String(mm).padStart(2, '0')}`;
  }

  function arrayToDate(arr: any[]): string {
    const [y, m, d] = arr;
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
  }

  function buildFormData(cols: any[], data: any): Record<string, any> {
    const values = data?.values || {};
    // const result: Record<string, any> = { id: data?.id ?? null };

    // const rawId = data?.id;
    // const result: Record<string, any> = { 
    //     id: (rawId !== undefined && rawId !== 0) ? rawId : null 
    // };

    const result: Record<string, any> = {};

const rawId = data?.id;

if (
    rawId !== undefined &&
    rawId !== null &&
    rawId !== "" &&
    rawId !== 0
) {
    result.id = rawId;
}

    cols.forEach(col => {
      try {
        const raw = values[col.name] ?? null;
        switch (col.type) {
          case 'OBJECT': {
            const src = raw?.data ?? raw ?? {};
            result[col.name] = {};
            col.subColumns?.forEach((sub: any) => {
              const subRaw = src[sub.name] ?? null;
              if (Array.isArray(subRaw) && sub.type === 'DATETIME') {
                result[col.name][sub.name] = arrayToDatetimeLocal(subRaw);
              } else if (Array.isArray(subRaw) && sub.type === 'DATE') {
                result[col.name][sub.name] = arrayToDate(subRaw);
              } else {
                result[col.name][sub.name] = subRaw ?? sub.defaultValue ?? '';
              }
            });
            break;
          }
          case 'DATETIME':
            result[col.name] = Array.isArray(raw) ? arrayToDatetimeLocal(raw) : (raw ?? '');
            break;
          case 'DATE':
            result[col.name] = Array.isArray(raw) ? arrayToDate(raw) : (raw ?? '');
            break;
          case 'TIME':
            result[col.name] = typeof raw === 'string'
              ? (raw.includes('T') ? raw.split('T')[1].substring(0, 5) : raw.substring(0, 5))
              : '';
            break;
          case 'RELATION':
            result[col.name] = raw && typeof raw === 'object' && !Array.isArray(raw)
              ? (raw.id ?? null)
              : (raw ?? null);
            break;
          case 'MULTI_RELATION':
            result[col.name] = Array.isArray(raw)
              ? raw.map((i: any) => typeof i === 'object' ? (i.id ?? i) : i)
              : [];
            break;
          case 'NUMBER':
          case 'RANGE':
            result[col.name] = typeof raw === 'number' ? raw : (raw ?? 0);
            break;
          case 'CHECKBOX':
            result[col.name] = typeof raw === 'boolean' ? raw : false;
            break;              
          case 'ARRAY':
          case 'MULTI_SELECT':
            result[col.name] = Array.isArray(raw)
              ? raw
              : (typeof raw === 'string' && raw
                  ? raw.split(',').map((s: string) => s.trim()).filter(Boolean)
                  : []);
            break;
          case "COLLECTION": {
    if (raw == null) {
        result[col.name] = [];
    } else if (Array.isArray(raw)) {
        result[col.name] = raw;
    } else if (typeof raw === "object") {
        // Map
        result[col.name] = Object.entries(raw).map(([key, value]) => ({
            key,
            value
        }));
    } else {
        result[col.name] = [raw];
    }
    break;
}  
          default:
            result[col.name] = raw !== null && typeof raw === 'object'
              ? (Array.isArray(raw) ? raw.join(', ') : (raw.displayField ?? raw.id ?? JSON.stringify(raw)))
              : (raw ?? col.defaultValue ?? '');
        }
      } catch (e) {
        console.error(`[DynamicForm] Failed to map field "${col.name}" (${col.type}):`, e);
        result[col.name] = null;
      }
    });

    return result;
  }

  // ─── Event handlers ──────────────────────────────────────────────────────

  function handleFieldChange(colName: string, value: any) {
    formData = { ...formData, [colName]: value };
  }

  function handleFileClear(colName: string, index: number | null) {
    const current = formData[colName];
    //  When a file is removed from the form, also remove from session tracking
    // so cancel doesn't try to delete something already gone
    if (typeof current === 'string') {
      sessionUploadedUrls.delete(current);
    } else if (Array.isArray(current) && index !== null) {
      sessionUploadedUrls.delete(current[index]);
    }

    if (Array.isArray(current)) {
      formData = { ...formData, [colName]: current.filter((_: any, i: number) => i !== index) };
    } else {
      formData = { ...formData, [colName]: null };
    }
  }

  //  Called by FieldRenderer when FileUploader completes an upload
  function handleFileUploaded(colName: string, url: string) {
    sessionUploadedUrls = new Set([...sessionUploadedUrls, url]);
  }

  function handleSubmit() {
    // On successful submit, clear session tracking — files are now saved
    sessionUploadedUrls = new Set();
    onSubmit(formData);
  }


// Helper to find the lookup descriptor for a specific field
  $: getLookup = (fieldName:any) => initialData?.relatedResources?.[fieldName]?.lookupDescriptor;


</script>

<div class="form-container !p-0">
  <div class="grid grid-cols-1 md:grid-cols-2 gap-6 p-8">
    {#each fields as col (col.name)}
      {@const colErrors = externalErrors[col.name]}
      {@const isWide = [
        'TEXTAREA','JSON','OBJECT','ARRAY','WYSIWYG',
        'RELATION','MULTI_RELATION','VIDEO','IMAGE',
        'FILE','AUDIO','DOCUMENT'
      ].includes(col.type)}

      <div class="flex flex-col gap-1.5 {isWide ? 'md:col-span-2' : ''}">
        <label class="field-label">
          {col.label}
          {#if col.required}<span class="text-danger">*</span>{/if}
        </label>

        <FieldRenderer
          {col}
          value={formData[col.name]}
          error={colErrors}
          on:change={(e) => handleFieldChange(col.name, e.detail.value)}
          on:fileclear={(e) => handleFileClear(col.name, e.detail.index)}
          on:fileuploaded={(e) => handleFileUploaded(col.name, e.detail.url)}
          lookup={getLookup(col.name)}
        />

        {#if colErrors?.length}
          <div class="flex flex-col gap-0.5 mt-0.5">
            {#each colErrors as err}
              <span class="text-danger text-[10px] font-bold uppercase tracking-tight flex items-center gap-1">
                <svg class="w-3 h-3 flex-shrink-0" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clip-rule="evenodd"/>
                </svg>
                {err}
              </span>
            {/each}
          </div>
        {/if}
      </div>
    {/each}
  </div>

  <div class="form-footer px-8">
    <button type="button" on:click={handleCancel} class="btn-secondary">
      Cancel
    </button>
    <button type="button" on:click={handleSubmit} class="btn-primary">
      {submitLabel} {resourceName}
    </button>
  </div>
</div>