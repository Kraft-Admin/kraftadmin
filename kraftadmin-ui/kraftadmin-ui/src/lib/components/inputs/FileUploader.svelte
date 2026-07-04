<!-- <script lang="ts">
  import { createEventDispatcher } from 'svelte';

  interface FileConfig { multiple: boolean; allowedMimeTypes: string[]; }
  interface Column {
    name: string;
    type: 'IMAGE' | 'VIDEO' | 'FILE' | 'AUDIO' | 'DOCUMENT';
    fileConfig?: FileConfig;
  }

  export let col: Column;
  export let value: string | string[] | null = null;
  
  // Internal state
  let uploading = false;
  const dispatch = createEventDispatcher<{ change: { value: any } }>();

  // --- Logic Moved from DynamicForm ---
  async function handleFileChange(event: Event) {
    const target = event.target as HTMLInputElement;
    const files = target.files;
    if (!files || files.length === 0) return;

    const isMultiple = col.fileConfig?.multiple ?? false;
    uploading = true;

    const fd = new FormData();
    if (isMultiple) { 
      Array.from(files).forEach(f => fd.append('files', f)); 
    } else { 
      fd.append('file', files[0]); 
      if (value && typeof value === 'string') fd.append('oldUrl', value); 
    }

    try {
      const response = await fetch('/admin/api/uploads', { method: 'POST', body: fd });
      if (!response.ok) throw new Error('Upload failed');
      const result = await response.json();
      const newValue = isMultiple ? [...(Array.isArray(value) ? value : []), ...result.urls] : result.url;
      dispatch('change', { value: newValue });
    } catch (err) {
      console.error("Upload failed", err);
    } finally { 
      uploading = false; 
    }
  }

  async function deleteFileFromServer(url: string) {
    try { await fetch('/admin/api/uploads', { method: 'DELETE', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ url }) }); } catch (err) { console.error(err); }
  }

  export async function performCleanup() {
    if (Array.isArray(value)) {
        for (const url of value) await deleteFileFromServer(url);
    } else if (value) {
        await deleteFileFromServer(value);
    }
}

  async function handleFileRemoval(index: number | null) {
    const val = value;
    if (Array.isArray(val)) {
      const url = val[index!];
      if (url) await deleteFileFromServer(url);
      dispatch('change', { value: val.filter((_, i) => i !== index) });
    } else {
      if (val && typeof val === 'string') await deleteFileFromServer(val);
      dispatch('change', { value: null });
    }
  }

  // --- UI Helpers ---
  const getExt = (val: any) => (typeof val !== 'string') ? '' : val.split('.').pop()?.toLowerCase() || '';
  const getFileName = (val: any) => (typeof val !== 'string') ? 'File' : val.substring(val.lastIndexOf('/') + 1) || 'File';
  function getPreviewType(val: any) {
    const ext = getExt(val);
    if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg'].includes(ext)) return 'IMAGE';
    if (['mp4', 'webm', 'mov'].includes(ext)) return 'VIDEO';
    if (['mp3', 'wav', 'ogg', 'aac', 'flac'].includes(ext)) return 'AUDIO';
    return 'DOCUMENT';
  }

  $: displayList = Array.isArray(value) ? value : (value ? [value] : []);
</script>

<div class="space-y-2">
  <div class="relative group w-full h-32 border-2 border-dashed border-border-subtle rounded-lg flex items-center justify-center overflow-hidden {uploading ? 'opacity-50 pointer-events-none' : ''}">
    
    {#if displayList.length > 0}
      <div class="absolute inset-0 flex gap-2 p-2 {col.fileConfig?.multiple ? 'overflow-x-auto' : ''}">
        {#each displayList as val, i}
          <div class="relative {col.fileConfig?.multiple ? 'w-24' : 'w-full'} h-full bg-zinc-900 rounded-lg overflow-hidden border border-zinc-700">
            {#if getPreviewType(val) === 'IMAGE'}
              <img src={val} alt="Preview" class="w-full h-full object-cover" />
            {:else if getPreviewType(val) === 'VIDEO'}
              <video src={val} class="w-full h-full object-cover" />
              <div class="absolute inset-0 flex items-center justify-center bg-black/20"><span class="bg-black/50 px-1.5 py-0.5 rounded text-[9px] text-white">VIDEO</span></div>
            {:else if getPreviewType(val) === 'AUDIO'}
              <div class="w-full h-full flex flex-col items-center justify-center p-1 text-zinc-400">
                <svg class="w-6 h-6 mb-1" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3"/></svg>
                <span class="text-[9px] uppercase">{getExt(val)}</span>
              </div>
            {:else}
              <div class="w-full h-full flex flex-col items-center justify-center p-1 text-zinc-400">
                <span class="text-xs font-bold uppercase">{getExt(val)}</span>
                <span class="text-[9px] truncate max-w-full px-1">{getFileName(val)}</span>
              </div>
            {/if}
            <button type="button" on:click={() => handleFileRemoval(i)}
              class="absolute top-1 right-1 bg-red-500 text-white text-[8px] px-1.5 py-0.5 rounded z-20 hover:bg-red-600 shadow-md">X</button>
          </div>
        {/each}
      </div>
    {:else}
      <div class="flex flex-col items-center pointer-events-none">
        <svg class="w-8 h-8 text-zinc-500 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"/></svg>
        <span class="text-xs text-zinc-400">Upload {col.type.toLowerCase()}</span>
      </div>
    {/if}

    <input type="file" multiple={col.fileConfig?.multiple ?? false} on:change={handleFileChange}
      class="absolute inset-0 w-full h-full opacity-0 cursor-pointer z-10" />
  </div>
</div> -->

<script lang="ts">
  import { createEventDispatcher } from 'svelte';

  export let col: any;
  export let value: any = null;

  const dispatch = createEventDispatcher();

  let uploading = false;
  let dragOver = false;
  let error: string | null = null;

  // ─── Derived ────────────────────────────────────────────────────────────

  $: isMultiple = col.multiple === true;
  $: files = isMultiple
    ? (Array.isArray(value) ? value.filter(Boolean) : [])
    : null;

  $: accept = col.type === 'IMAGE' ? 'image/*'
    : col.type === 'VIDEO' ? 'video/*'
    : col.type === 'AUDIO' ? 'audio/*'
    : col.type === 'DOCUMENT' ? '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx'
    : '*';

  $: isImage = col.type === 'IMAGE';
  $: isVideo = col.type === 'VIDEO';

  // ─── Upload ──────────────────────────────────────────────────────────────

  async function uploadFile(file: File): Promise<string | null> {
    const fd = new FormData();
    fd.append('file', file);

    // Pass old URL only for single-file replace so the server can clean up
    if (!isMultiple && typeof value === 'string' && value) {
      fd.append('oldUrl', value);
    }

    try {
      const res = await fetch('/admin/api/uploads', { method: 'POST', body: fd });
      if (!res.ok) throw new Error(`Upload failed: ${res.status}`);
      const data = await res.json();
      return data.url as string;
    } catch (e: any) {
      error = e.message ?? 'Upload failed';
      return null;
    }
  }

  async function handleFiles(fileList: FileList | null) {
    if (!fileList || fileList.length === 0) return;
    error = null;
    uploading = true;

    try {
      if (isMultiple) {
        const uploads = await Promise.all(Array.from(fileList).map(uploadFile));
        const succeeded = uploads.filter(Boolean) as string[];
        const next = [...(files ?? []), ...succeeded];
        succeeded.forEach(url => dispatch('uploaded', { url }));
        dispatch('change', { value: next });
      } else {
        const url = await uploadFile(fileList[0]);
        if (url) {
          dispatch('uploaded', { url }); // ✅ DynamicForm tracks this for cancel cleanup
          dispatch('change', { value: url });
        }
      }
    } finally {
      uploading = false;
    }
  }

  function onInputChange(e: Event) {
    handleFiles((e.target as HTMLInputElement).files);
  }

  // ─── Delete ──────────────────────────────────────────────────────────────

  async function deleteFile(url: string) {
    try {
      await fetch('/admin/api/uploads', {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url })
      });
    } catch (e) {
      console.error('[FileUploader] Delete failed:', url, e);
    }
  }

  async function clearSingle() {
    if (typeof value === 'string' && value) await deleteFile(value);
    dispatch('clear', { index: null });
    dispatch('change', { value: null });
  }

  async function clearMultiItem(index: number) {
    const url = files?.[index];
    if (url) await deleteFile(url);
    dispatch('clear', { index });
    dispatch('change', { value: (files ?? []).filter((_: any, i: number) => i !== index) });
  }

  // ─── Drag & Drop ──────────────────────────────────────────────────────────

  function onDragOver(e: DragEvent) {
    e.preventDefault();
    dragOver = true;
  }

  function onDragLeave() {
    dragOver = false;
  }

  function onDrop(e: DragEvent) {
    e.preventDefault();
    dragOver = false;
    handleFiles(e.dataTransfer?.files ?? null);
  }
</script>

<!-- ─── Single file ──────────────────────────────────────────────────── -->
{#if !isMultiple}
  <div
    class="relative group w-full h-36 border-2 border-dashed rounded-xl flex flex-col items-center justify-center transition-all duration-200 cursor-pointer overflow-hidden
      {dragOver ? 'border-brand-primary bg-brand-primary/5' : 'border-border-subtle hover:border-brand-primary/50'}
      {uploading ? 'opacity-60 pointer-events-none' : ''}"
    on:dragover={onDragOver}
    on:dragleave={onDragLeave}
    on:drop={onDrop}
    role="button"
    tabindex="0"
  >
    {#if value && isImage}
      <!-- Preview -->
      <img src={value} alt="Preview" class="absolute inset-0 w-full h-full object-cover" />
      <!-- Hover overlay -->
      <div class="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-3">
        <label class="btn-xs btn-secondary cursor-pointer">
          Replace
          <input type="file" {accept} class="sr-only" on:change={onInputChange} />
        </label>
        <button type="button" class="btn-xs btn-danger" on:click|stopPropagation={clearSingle}>
          Remove
        </button>
      </div>

    {:else if value && isVideo}
      <video src={value} class="absolute inset-0 w-full h-full object-cover" muted />
      <div class="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-3">
        <label class="btn-xs btn-secondary cursor-pointer">
          Replace
          <input type="file" {accept} class="sr-only" on:change={onInputChange} />
        </label>
        <button type="button" class="btn-xs btn-danger" on:click|stopPropagation={clearSingle}>
          Remove
        </button>
      </div>

    {:else if value}
      <!-- Non-previewable file -->
      <div class="flex flex-col items-center gap-2 p-4">
        <svg class="w-8 h-8 text-brand-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
        </svg>
        <span class="text-xs text-text-secondary truncate max-w-full px-4">{value.split('/').pop()}</span>
        <div class="flex gap-2">
          <label class="btn-xs btn-secondary cursor-pointer">
            Replace
            <input type="file" {accept} class="sr-only" on:change={onInputChange} />
          </label>
          <button type="button" class="btn-xs btn-danger" on:click|stopPropagation={clearSingle}>
            Remove
          </button>
        </div>
      </div>

    {:else}
      <!-- Empty drop zone -->
      <label class="flex flex-col items-center gap-2 cursor-pointer w-full h-full items-center justify-center">
        {#if uploading}
          <div class="w-6 h-6 border-2 border-brand-primary border-t-transparent rounded-full animate-spin"></div>
          <span class="text-xs text-text-muted">Uploading...</span>
        {:else}
          <svg class="w-8 h-8 text-text-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"/>
          </svg>
          <span class="text-xs text-text-muted">
            Drop {col.type.toLowerCase()} or <span class="text-brand-primary underline">browse</span>
          </span>
        {/if}
        <input type="file" {accept} class="sr-only" on:change={onInputChange} disabled={uploading} />
      </label>
    {/if}
  </div>

<!-- ─── Multiple files ─────────────────────────────────────────────────── -->
{:else}
  <div class="space-y-3">
    <!-- Existing files grid -->
    {#if files && files.length > 0}
      <div class="grid grid-cols-3 gap-2">
        {#each files as fileUrl, i}
          <div class="relative group aspect-square rounded-lg overflow-hidden border border-border-subtle bg-bg-main">
            {#if isImage}
              <img src={fileUrl} alt="Upload {i+1}" class="w-full h-full object-cover" />
            {:else}
              <div class="w-full h-full flex flex-col items-center justify-center gap-1 p-2">
                <svg class="w-6 h-6 text-brand-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                </svg>
                <span class="text-[10px] text-text-muted truncate w-full text-center">{fileUrl.split('/').pop()}</span>
              </div>
            {/if}
            <!-- Per-item remove -->
            <button
              type="button"
              class="absolute top-1 right-1 w-6 h-6 rounded-full bg-danger/90 text-white opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center text-xs font-bold"
              on:click={() => clearMultiItem(i)}
            >
              &times;
            </button>
          </div>
        {/each}
      </div>
    {/if}

    <!-- Add more drop zone -->
    <div
      class="w-full h-24 border-2 border-dashed rounded-xl flex items-center justify-center transition-all duration-200
        {dragOver ? 'border-brand-primary bg-brand-primary/5' : 'border-border-subtle hover:border-brand-primary/50'}
        {uploading ? 'opacity-60 pointer-events-none' : ''}"
      on:dragover={onDragOver}
      on:dragleave={onDragLeave}
      on:drop={onDrop}
      role="button"
      tabindex="0"
    >
      <label class="flex items-center gap-2 cursor-pointer text-sm text-text-muted">
        {#if uploading}
          <div class="w-4 h-4 border-2 border-brand-primary border-t-transparent rounded-full animate-spin"></div>
          Uploading...
        {:else}
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          Add {col.type.toLowerCase()}
        {/if}
        <input type="file" {accept} multiple class="sr-only" on:change={onInputChange} disabled={uploading} />
      </label>
    </div>
  </div>
{/if}

<!-- ─── Upload progress bar ───────────────────────────────────────────── -->
{#if uploading}
  <div class="mt-2 w-full h-1 bg-border-subtle rounded-full overflow-hidden">
    <div class="h-full bg-brand-primary animate-pulse w-full"></div>
  </div>
{/if}

<!-- ─── Error ─────────────────────────────────────────────────────────── -->
{#if error}
  <p class="text-danger text-xs mt-1">{error}</p>
{/if}