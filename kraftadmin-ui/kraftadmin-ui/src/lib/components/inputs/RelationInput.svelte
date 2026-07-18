<script lang="ts">
    import { createEventDispatcher, onMount } from 'svelte';
  import type { LabelledObject } from '../../types/utils';
  import type { RelatedCollection } from '../../types/resources';

    export let value: string | null = null;
    
    export let lookup: any = null;
    // export let relation: RelatedCollection;

    // const lookup = relation.lookupDescriptor;

    console.log("lookup ", lookup);

    const dispatch = createEventDispatcher();

    let selectedLabel = '';
    let searchTerm = '';
    let results: LabelledObject[] = [];
    let open = false;
    let debounce: any;
    let container: HTMLElement;
    let input: HTMLInputElement;

    let resolvedValue: string | null = null;

$: if (
    value &&
    value !== resolvedValue &&
    !selectedLabel
) {
    resolvedValue = value;
    resolveLabel(value);
}

  async function resolveLabel(id: string) {
    if (!lookup?.targetEntity || !id) return;

    try {

        const res = await fetch(
            `/admin/api/resources/${lookup.targetEntity}/lookup?ids=${encodeURIComponent(id)}`
        );

        if (!res.ok) return;

        const options: LabelledObject[] = await res.json();

        selectedLabel =
            options.find(o => o.id === id)?.label ?? `#${id}`;

        console.log("options ", options, selectedLabel)    

    } catch (e) {
        console.error(e);
        selectedLabel = `#${id}`;
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
            console.error('[RelationInput] Search failed:', e);
        }
    }

    // function select(option: any) {
    //     selectedLabel = String(
    //         option.label ??
    //         option.name ??
    //         `#${option.id}`
    //     );

    //     searchTerm = '';
    //     results = [];
    //     open = false;

    //     dispatch('change', option.id?.toString() ?? null);
    // }

    function select(option: LabelledObject) {
    // selectedLabel = option.label;

    searchTerm = '';
    results = [];
    open = false;

    dispatch('change', option.id);
}

    function clear() {
        selectedLabel = '';
        searchTerm = '';
        results = [];
        open = false;

        dispatch('change', null);

        input?.focus();
    }

    function onInput(e: Event) {
        searchTerm = (e.target as HTMLInputElement).value;

        // Start replacing current selection
        if (selectedLabel) {
            selectedLabel = '';
            dispatch('change', null);
        }

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

    <div class="relative">

        <input
            bind:this={input}
            type="text"
            class="input-base w-full pr-3"
            placeholder={selectedLabel ? '' : `Search by ${lookup?.searchableFields.join(", ")}...`}
            bind:value={searchTerm}
            on:input={onInput}
            on:focus={() => {
                open = true;
                if (searchTerm.trim()) search(searchTerm);
            }}
        />

        {#if selectedLabel && !searchTerm}
            <div class="absolute inset-y-0 left-3 flex items-center pointer-events-none">
                <span
                    class="inline-flex items-center rounded-full text-brand-primary px-2.5 py-1 text-xs font-medium border border-primary/20"
                >
                    {selectedLabel}
                </span>

                <!-- <span
    class="inline-flex items-center rounded-full bg-primary/10 text-primary px-2.5 py-1 text-xs font-medium border border-primary/20"
>
    {field}
</span>  -->
            </div>

            <button
                type="button"
                class="absolute right-3 top-1/2 -translate-y-1/2 text-text-muted hover:text-text-main"
                on:click|stopPropagation={clear}
                title="Clear selection"
            >
                &times;
            </button>
        {/if}

    </div>

    {#if open && results.length > 0}
        <div class="absolute z-50 mt-1 w-full overflow-y-auto rounded-lg border border-border-subtle bg-bg-surface shadow-xl max-h-60">

            {#each results as option (option.id)}

                <button
                    type="button"
                    class="w-full px-4 py-2.5 text-left text-sm text-brand-primary"
                    on:click={() => select(option)}
                >
                    {option.label}
                </button>
                
            {/each}

        </div>
    {/if}

    {#if open && searchTerm.trim() && results.length === 0}
        <div class="absolute z-50 mt-1 w-full rounded-lg border border-border-subtle bg-bg-surface p-3 shadow-xl">
            <p class="text-center text-xs text-text-muted">
                No results for "{searchTerm}"
            </p>
        </div>
    {/if}

</div>