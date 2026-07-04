<script lang="ts">
  import { createEventDispatcher } from 'svelte';
  export const value: string | null = null;
  export let lookup: any = null;

  const dispatch = createEventDispatcher();
  let searchTerm = '';
  let results: any[] = [];
  let open = false;
  let debounce: any;

  async function search(q: string) {
    if (!lookup?.targetEntity || !q) { results = []; return; }
    const res = await fetch(`/admin/api/resources/${lookup.targetEntity}/lookup/${lookup.searchField || 'name'}?search=${encodeURIComponent(q)}`);
    if (res.ok) results = await res.json();
  }

  function select(option: any) {
    searchTerm = option.displayField || option.name;
    open = false;
    results = [];
    dispatch('change', option.id);
  }

  function onInput(e: Event) {
    searchTerm = (e.target as HTMLInputElement).value;
    open = true;
    clearTimeout(debounce);
    debounce = setTimeout(() => search(searchTerm), 300);
  }
</script>

<div class="relative">
  <input
    type="text"
    class="input-base w-full"
    placeholder="Search..."
    value={searchTerm}
    on:input={onInput}
    on:focus={() => open = true}
  />
  {#if open && results.length > 0}
    <div class="absolute z-50 w-full mt-1 bg-bg-surface border border-border-subtle rounded-lg shadow-lg max-h-60 overflow-y-auto">
      {#each results as option}
        <button type="button" class="w-full text-left px-4 py-2 hover:bg-bg-main text-sm text-text-main" on:click={() => select(option)}>
          {option.displayField || option.name}
        </button>
      {/each}
    </div>
  {/if}
</div>