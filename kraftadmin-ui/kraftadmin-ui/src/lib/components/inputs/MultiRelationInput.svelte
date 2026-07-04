<script lang="ts">
  import { createEventDispatcher } from 'svelte';
  export let value: string[] = [];
  export let lookup: any = null;

  const dispatch = createEventDispatcher();
  let searchTerm = '';
  let results: any[] = [];
  let open = false;
  let debounce: any;
  let labels: Record<string, string> = {};

  async function search(q: string) {
    if (!lookup?.targetEntity || !q) { results = []; return; }
    const res = await fetch(`/admin/api/resources/${lookup.targetEntity}/lookup/${lookup.searchField || 'name'}?search=${encodeURIComponent(q)}`);
    if (res.ok) results = await res.json();
  }

  function select(option: any) {
    if (!value.includes(option.id)) {
      labels[option.id] = option.displayField || option.name;
      dispatch('change', [...value, option.id]);
    }
    searchTerm = '';
    results = [];
  }

  function remove(id: string) {
    dispatch('change', value.filter(v => v !== id));
  }

  function onInput(e: Event) {
    searchTerm = (e.target as HTMLInputElement).value;
    open = true;
    clearTimeout(debounce);
    debounce = setTimeout(() => search(searchTerm), 300);
  }
</script>

<div class="relative">
  <div class="input-base min-h-10.5 flex flex-wrap gap-2 p-2">
    {#each value as id}
      <div class="bg-brand-primary/10 text-brand-primary px-2 py-0.5 rounded-full text-xs flex items-center gap-1">
        {labels[id] || id}
        <button type="button" class="opacity-60 hover:opacity-100" on:click={() => remove(id)}>&times;</button>
      </div>
    {/each}
    <input
      type="text"
      class="bg-transparent border-none outline-none flex-1 min-w-30 text-sm"
      placeholder="Search to add..."
      value={searchTerm}
      on:input={onInput}
      on:focus={() => open = true}
    />
  </div>
  {#if open && results.length > 0}
    <div class="absolute z-50 w-full mt-1 bg-bg-surface border border-border-subtle rounded-lg shadow-lg max-h-60 overflow-y-auto">
      {#each results as option}
        <button type="button" class="w-full text-left px-4 py-2 hover:bg-bg-main text-sm text-text-main flex justify-between" on:click={() => select(option)}>
          {option.displayField || option.name}
          {#if value.includes(option.id)}<span class="text-brand-primary">✓</span>{/if}
        </button>
      {/each}
    </div>
  {/if}
</div>