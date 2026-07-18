<script lang="ts">
  import { createEventDispatcher } from 'svelte';
  import type { LabelledObject } from '../../types/utils';

  export let value: string[] = [];
  export let lookup: any = null;

  const dispatch = createEventDispatcher();

  let searchTerm = '';
  let results: LabelledObject[] = [];
  let open = false;
  let debounce: any;
  let container: HTMLElement;
  let labels: Record<string, string> = {};

  // Fetch labels for existing IDs when value changes
  $: if (value && lookup?.targetEntity) {
    const missingIds = value.filter(id => !labels[id]);
    if (missingIds.length > 0) {
      fetchLabels(missingIds);
    }
  }

  async function fetchLabels(ids: string[]) {
    if (!lookup?.targetEntity) return;

    try {
      // Use the same endpoint structure as single-relation: ?ids=id1,id2,id3
      const res = await fetch(
        `/admin/api/resources/${lookup.targetEntity}/lookup?ids=${encodeURIComponent(ids.join(','))}`
      );

      if (res.ok) {
        const options: LabelledObject[] = await res.json();
        options.forEach(opt => {
          labels[opt.id] = opt.label;
        });
        labels = { ...labels }; // Trigger reactivity
      }
    } catch (e) {
      console.error('[MultiRelationInput] fetchLabels failed:', e);
    }
  }

  async function search(q: string) {
    if (!lookup?.targetEntity || !q.trim()) {
      results = [];
      return;
    }
    try {
      const res = await fetch(
        `/admin/api/resources/${lookup.targetEntity}/lookup?search=${encodeURIComponent(q)}`
      );
      if (res.ok) {
        results = await res.json() as LabelledObject[];
      }
    } catch (e) {
      console.error('[MultiRelationInput] search failed:', e);
    }
  }

  function select(option: LabelledObject) {
    const id = option.id.toString();
    if (value.includes(id)) return;

    labels[id] = option.label; // Update local cache
    labels = { ...labels };

    dispatch('change', [...value, id]);
    searchTerm = '';
    results = [];
    open = false;
  }

  function remove(id: string) {
    dispatch('change', value.filter(v => v !== id));
  }

  function onInput(e: Event) {
    searchTerm = (e.target as HTMLInputElement).value;
    open = true;
    clearTimeout(debounce);
    if (!searchTerm.trim()) {
      results = [];
      return;
    }
    debounce = setTimeout(() => search(searchTerm), 300);
  }

  function onWindowClick(e: MouseEvent) {
    if (container && !container.contains(e.target as Node)) {
      open = false;
    }
  }
</script>

<svelte:window on:click={onWindowClick} />

<div class="relative" bind:this={container}>
  <div class="input-base min-h-[42px] flex flex-wrap gap-1.5 p-2 cursor-text" on:click={() => { open = true; }}>
    {#each value as id (id)}
      <span class="bg-brand-primary/10 text-brand-primary px-2.5 py-0.5 rounded-full text-xs font-medium flex items-center gap-1 shrink-0">
        <span class="truncate">{labels[id] ?? id}</span>
        <button type="button" class="opacity-60 hover:opacity-100" on:click|stopPropagation={() => remove(id)}>
          &times;
        </button>
      </span>
    {/each}

    <input
      type="text"
      class="bg-transparent border-none outline-none flex-1 min-w-[120px] text-sm"
      placeholder={`Search by ${lookup?.searchableFields.join(", ")}...`}
      bind:value={searchTerm}
      on:input={onInput}
      on:focus={() => open = true}
    />
  </div>

  {#if open && results.length > 0}
    <div class="absolute z-50 w-full mt-1 border border-border-subtle bg-bg-surface rounded-lg shadow-xl max-h-60 overflow-y-auto">
      {#each results as option (option.id)}
        <button type="button" class="w-full text-left px-4 py-2.5 text-brand-primary text-sm" on:click={() => select(option)}>
          {option.label}
        </button>
      {/each}
    </div>
  {/if}
</div>