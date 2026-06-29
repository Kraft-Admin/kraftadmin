<script lang="ts">
  import { createEventDispatcher } from 'svelte';

  interface FileConfig { multiple: boolean; allowedMimeTypes: string[]; }
  interface Column {
    name: string;
    type: 'IMAGE' | 'VIDEO' | 'FILE' | 'AUDIO' | 'DOCUMENT';
    fileConfig?: FileConfig;
  }

  export let col: Column;
  export let value: string | string[] | null = null;
  export let uploading: boolean = false;

  const dispatch = createEventDispatcher<{
    change: { files: FileList };
    clear: { index: number | null };
  }>();

  function handleFileChange(event: Event) {
    const target = event.target as HTMLInputElement;
    if (target.files) dispatch('change', { files: target.files });
  }

  function clearFile(e: MouseEvent, index: number) {
    e.stopPropagation();
    dispatch('clear', { index });
  }

  const getExt = (url: string) => url.split('.').pop()?.toLowerCase() || '';
  const getFileName = (url: string) => url?.substring(url.lastIndexOf('/') + 1) || 'File';

  // Determine the display type based on extension
  function getPreviewType(url: string) {
    const ext = getExt(url);
    if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg'].includes(ext)) return 'IMAGE';
    if (['mp4', 'webm', 'mov'].includes(ext)) return 'VIDEO';
    if (['mp3', 'wav', 'ogg', 'aac', 'flac'].includes(ext)) return 'AUDIO';
    return 'DOCUMENT';
  }
</script>

<div class="space-y-2">
  <div class="relative group w-full h-32 border-2 border-dashed border-border-subtle rounded-lg flex items-center justify-center overflow-hidden {uploading ? 'opacity-50 pointer-events-none' : ''}">
    
    {#if value && (Array.isArray(value) ? value.length > 0 : value)}
      <div class="absolute inset-0 flex gap-2 p-2 {col.fileConfig?.multiple ? 'overflow-x-auto' : ''}">
        {#each (Array.isArray(value) ? value : [value]) as val, i}
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

            <button type="button" on:click={(e) => clearFile(e, i)}
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
</div>