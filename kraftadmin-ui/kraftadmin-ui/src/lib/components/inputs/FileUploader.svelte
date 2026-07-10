<script lang="ts">
  import { createEventDispatcher, onMount } from 'svelte';

  export let col: any;
  export let value: any = null;

  const dispatch = createEventDispatcher();

  // ─── Derived from col.fileOptions ────────────────────────────────────────
  $: fileOptions = col?.fileOptions ?? {};
  $: isMultiple = fileOptions.multiple === true;
  $: maxFiles = fileOptions.maxFiles ?? (isMultiple ? 10 : 1);
  $: maxSizeBytes = fileOptions.maxSizeBytes ?? 10 * 1024 * 1024;
  $: allowedExtensions = fileOptions.allowedExtensions as string[] ?? [];
  $: allowedMimeTypes = fileOptions.allowedMimeTypes as string[] ?? [];

  // ─── Accept string for the file input ────────────────────────────────────
  $: accept = allowedMimeTypes.length > 0
    ? allowedMimeTypes.join(',')
    : allowedExtensions.length > 0
      ? allowedExtensions.map(e => `.${e}`).join(',')
      : col?.type === 'IMAGE' ? 'image/*'
      : col?.type === 'VIDEO' ? 'video/*'
      : col?.type === 'AUDIO' ? 'audio/*'
      : '*';

  $: isImage = col?.type === 'IMAGE';
  $: isVideo = col?.type === 'VIDEO';

  $: {
    console.log("Uploader value", value);
    console.log("Uploader files", files);
}

  // ─── State ────────────────────────────────────────────────────────────────
  let uploading = false;
  let uploadingIndexes = new Set<number>(); // which slots are currently uploading
  let dragOver = false;
  let errors: string[] = [];
  let container: HTMLElement;

  // Normalise value to always work with an array internally
  // Single mode: value is string | null
  // Multi mode:  value is string[]
  $: files1 = isMultiple
    ? (Array.isArray(value) ? (value as string[]).filter(Boolean) : [])
    : (value ? [value as string] : []);

$: files = normalizeFiles(value);

function normalizeFiles(value: any): string[] {
    if (value == null) {
        return [];
    }

    if (Array.isArray(value)) {
        return value
            .map(v => String(v).trim())
            .filter(Boolean);
    }

    if (typeof value === "string") {
        const trimmed = value.trim();

        if (!trimmed) {
            return [];
        }

        // JSON array stored as string
        if (trimmed.startsWith("[")) {
            try {
                const parsed = JSON.parse(trimmed);
                if (Array.isArray(parsed)) {
                    return parsed
                        .map(v => String(v).trim())
                        .filter(Boolean);
                }
            } catch {
                // Ignore and fall back
            }
        }

        // Comma-separated string
        return trimmed
            .split(",")
            .map(v => v.trim())
            .filter(Boolean);
    }

    return [];
}

  // ─── Validation ───────────────────────────────────────────────────────────

  function validateFile(file: File): string | null {
    if (maxSizeBytes > 0 && file.size > maxSizeBytes) {
      return `${file.name} exceeds max size of ${formatBytes(maxSizeBytes)}`;
    }
    if (allowedExtensions.length > 0) {
      const ext = file.name.split('.').pop()?.toLowerCase() ?? '';
      if (!allowedExtensions.includes(ext)) {
        return `${file.name} — allowed types: ${allowedExtensions.join(', ')}`;
      }
    }
    if (allowedMimeTypes.length > 0 && !allowedMimeTypes.includes(file.type)) {
      return `${file.name} has unsupported type (${file.type})`;
    }
    return null;
  }

  function formatBytes(bytes: number): string {
    if (bytes >= 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
    if (bytes >= 1024) return `${(bytes / 1024).toFixed(0)} KB`;
    return `${bytes} B`;
  }

  // ─── Upload helpers ───────────────────────────────────────────────────────

  async function uploadSingle(file: File, oldUrl?: string): Promise<string | null> {
    const fd = new FormData();
    fd.append('file', file);
    if (oldUrl) fd.append('oldUrl', oldUrl);

    const res = await fetch('/admin/api/uploads', { method: 'POST', body: fd });
    if (!res.ok) throw new Error(`Upload failed: ${res.status}`);
    const data = await res.json();
    return data.url as string;
  }

  async function uploadMultiple(filesToUpload: File[]): Promise<string[]> {
    const fd = new FormData();
    filesToUpload.forEach(f => fd.append('files', f));

    const res = await fetch('/admin/api/uploads', { method: 'POST', body: fd });
    if (!res.ok) throw new Error(`Upload failed: ${res.status}`);
    const data = await res.json();
    return data.urls as string[];
  }

  // ─── Main file handler ────────────────────────────────────────────────────

  async function handleFiles(incoming: FileList | File[]) {
    errors = [];
    const fileArray = Array.from(incoming);

    // Validate all files first
    const validationErrors = fileArray.map(validateFile).filter(Boolean) as string[];
    if (validationErrors.length > 0) {
      errors = validationErrors;
      return;
    }

    // Enforce maxFiles for multi mode
    if (isMultiple) {
      const remaining = maxFiles - files.length;
      if (remaining <= 0) {
        errors = [`Maximum ${maxFiles} file(s) allowed`];
        return;
      }
      const allowed = fileArray.slice(0, remaining);
      if (allowed.length < fileArray.length) {
        errors = [`Only ${remaining} more file(s) can be added (max ${maxFiles})`];
      }
      await uploadMultipleFiles(allowed);
    } else {
      await uploadSingleFile(fileArray[0]);
    }
  }

  async function uploadSingleFile(file: File) {
    uploading = true;
    try {
      const oldUrl = files[0] ?? undefined;
      const url = await uploadSingle(file, oldUrl);
      if (url) {
        dispatch('uploaded', { url });
        dispatch('change', { value: url });
      }
    } catch (e: any) {
      errors = [e.message ?? 'Upload failed'];
    } finally {
      uploading = false;
    }
  }

  async function uploadMultipleFiles(newFiles: File[]) {
    uploading = true;
    try {
      let urls: string[];

      if (newFiles.length === 1) {
        // Use single endpoint for one file
        const url = await uploadSingle(newFiles[0]);
        urls = url ? [url] : [];
      } else {
        urls = await uploadMultiple(newFiles);
      }

      const next = [...files, ...urls];
      urls.forEach(url => dispatch('uploaded', { url }));
      dispatch('change', { value: next });
    } catch (e: any) {
      errors = [e.message ?? 'Upload failed'];
    } finally {
      uploading = false;
    }
  }

  // ─── Delete ───────────────────────────────────────────────────────────────

  async function deleteFile(url: string) {
    try {
      await fetch('/admin/api/uploads', {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url })
      });
    } catch (e) {
      console.error('[FileUploader] delete failed:', url, e);
    }
  }

  async function removeSingle() {
    const url = files[0];
    if (url) await deleteFile(url);
    dispatch('clear', { index: null });
    dispatch('change', { value: null });
  }

  async function removeItem(index: number) {
    const url = files[index];
    if (url) await deleteFile(url);
    const next = files.filter((_, i) => i !== index);
    dispatch('clear', { index });
    dispatch('change', { value: next });
  }

  // ─── Input / drag events ──────────────────────────────────────────────────

  function onInputChange(e: Event) {
    const input = e.target as HTMLInputElement;
    if (input.files?.length) handleFiles(input.files);
    input.value = ''; // reset so same file can be re-selected
  }

  function onDragOver(e: DragEvent) { e.preventDefault(); dragOver = true; }
  function onDragLeave() { dragOver = false; }
  function onDrop(e: DragEvent) {
    e.preventDefault();
    dragOver = false;
    if (e.dataTransfer?.files.length) handleFiles(e.dataTransfer.files);
  }
</script>

<!-- SINGLE FILE MODE -->
{#if !isMultiple}
<div
    class="relative group w-full h-36 border-2 border-dashed rounded-xl flex items-center justify-center overflow-hidden transition-all duration-200
    {dragOver ? 'border-brand-primary bg-brand-primary/5' : 'border-border-subtle hover:border-brand-primary/50'}
    {uploading ? 'opacity-60 pointer-events-none' : ''}"
    on:dragover={onDragOver}
    on:dragleave={onDragLeave}
    on:drop={onDrop}
>

    {#if files[0] && isImage}
        <img src={files[0]} alt="Preview" class="absolute inset-0 w-full h-full object-cover" />

        <div class="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
            <label class="px-3 py-1 rounded-lg bg-white text-black cursor-pointer text-xs font-semibold">
                Replace
                <input type="file" {accept} class="hidden" on:change={onInputChange}/>
            </label>

            <button
                type="button"
                class="px-3 py-1 rounded-lg bg-red-600 text-white text-xs"
                on:click|stopPropagation={removeSingle}>
                Remove
            </button>
        </div>

    {:else if files[0]}

        <div class="flex flex-col items-center gap-2">
            <svg class="w-8 h-8 text-brand-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
            </svg>

            <span class="text-xs truncate">{files[0].split('/').pop()}</span>

            <div class="flex gap-2">
                <label class="px-3 py-1 rounded-lg bg-bg-surface cursor-pointer text-xs">
                    Replace
                    <input type="file" {accept} class="hidden" on:change={onInputChange}/>
                </label>

                <button
                    type="button"
                    class="px-3 py-1 rounded-lg bg-danger text-white text-xs"
                    on:click|stopPropagation={removeSingle}>
                    Remove
                </button>
            </div>
        </div>

    {:else}

        <label class="flex flex-col items-center justify-center w-full h-full cursor-pointer gap-2">

            {#if uploading}
                <div class="w-6 h-6 border-2 border-brand-primary border-t-transparent rounded-full animate-spin"></div>
            {:else}
                <svg class="w-8 h-8 text-text-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"/>
                </svg>

                <span class="text-xs text-center text-text-main">
                    Drop file here or
                    <span class="underline text-brand-primary">browse</span>
                </span>
            {/if}

            <input
                type="file"
                {accept}
                class="hidden"
                on:change={onInputChange}
                disabled={uploading}
            />

        </label>

    {/if}
</div>

<!-- MULTIPLE FILE MODE -->

{:else}

<div class="flex flex-col gap-3 w-full">

    <!-- Upload area -->
    {#if files.length < maxFiles}
        <label
            class="relative border-2 border-dashed rounded-xl min-h-[120px] p-6 flex flex-col items-center justify-center cursor-pointer transition-all
            {dragOver ? 'border-brand-primary bg-brand-primary/5' : 'border-border-subtle hover:border-brand-primary/50'}
            {uploading ? 'opacity-60 pointer-events-none' : ''}"
            on:dragover={onDragOver}
            on:dragleave={onDragLeave}
            on:drop={onDrop}
        >

            {#if uploading}
                <div class="w-6 h-6 border-2 border-brand-primary border-t-transparent rounded-full animate-spin"></div>
                <span class="text-xs mt-2">Uploading...</span>

            {:else}

                <svg class="w-8 h-8 text-text-muted mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M12 4v16m8-8H4"/>
                </svg>

                <div class="text-sm text-text-main">
                    Drop files here or
                    <span class="underline text-brand-primary">browse</span>
                </div>

                <div class="text-[11px] text-text-muted mt-1">
                    {files.length} / {maxFiles} uploaded
                </div>

            {/if}

            <input
                type="file"
                {accept}
                multiple
                class="hidden"
                on:change={onInputChange}
                disabled={uploading}
            />

        </label>
    {/if}

    <!-- Preview grid -->
    {#if files.length > 0}

        <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3">

            {#each files as fileUrl, i (fileUrl)}

                <div class="relative rounded-xl border bg-bg-main overflow-hidden">

                    {#if isImage}

                        <img
                            src={fileUrl}
                            alt=""
                            class="w-full aspect-square object-cover"
                        />

                    {:else if isVideo}

                        <!-- svelte-ignore a11y-media-has-caption -->
                        <video
                            src={fileUrl}
                            class="w-full aspect-square object-cover"
                            muted
                        />

                    {:else}

                        <div class="aspect-square flex flex-col items-center justify-center p-3">
                            <svg class="w-8 h-8 text-brand-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                    d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                            </svg>

                            <span class="text-[10px] truncate w-full text-center mt-2">
                                {fileUrl.split('/').pop()}
                            </span>
                        </div>

                    {/if}

                    <button
                        type="button"
                        class="absolute top-2 right-2 w-6 h-6 rounded-full bg-red-600 text-white flex items-center justify-center"
                        on:click={() => removeItem(i)}
                    >
                        ×
                    </button>

                    {#if isImage}
                        <a
                            href={fileUrl}
                            target="_blank"
                            class="absolute bottom-2 left-2 text-xs bg-black/60 text-white rounded px-2 py-1"
                        >
                            View
                        </a>
                    {/if}

                </div>

            {/each}

        </div>

    {/if}

</div>

{/if}

<!-- Upload progress -->

{#if uploading}
<div class="mt-2 h-1 bg-border-subtle rounded-full overflow-hidden">
    <div class="h-full bg-brand-primary animate-pulse w-full"></div>
</div>
{/if}

<!-- Errors -->

{#if errors.length}
<div class="mt-2 space-y-1">
    {#each errors as error}
        <div class="text-xs text-danger">{error}</div>
    {/each}
</div>
{/if}