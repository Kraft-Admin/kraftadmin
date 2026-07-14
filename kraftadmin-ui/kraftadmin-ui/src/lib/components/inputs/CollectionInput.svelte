<script lang="ts">
    import { createEventDispatcher } from "svelte";
    import SmartInput from "./SmartInput.svelte";
    import {
        ElementCollectionShape,
        type ElementCollectionDescriptor
    } from "../../types/descriptors";
    import { createDefaultValue } from "../../stores/createDefault";

    export let descriptor: ElementCollectionDescriptor;
    export let value: any[] = [];

    const dispatch = createEventDispatcher();

    function emit() {
        dispatch("change", value);
    }

    function addItem() {
        value = [...value, createDefaultValue(descriptor.value)];
        emit();
    }

    function addEntry() {
        value = [
            ...value,
            {
                key: createDefaultValue(descriptor.key!),
                value: createDefaultValue(descriptor.value)
            }
        ];
        emit();
    }

    function remove(index: number) {
        value = value.filter((_, i) => i !== index);
        emit();
    }

    $: emit();
</script>

<div class="space-y-4">
    {#if descriptor.shape === ElementCollectionShape.MAP}
        {#each value as entry, i (i)}
            <div class="grid grid-cols-2 gap-4 items-end">
                <SmartInput descriptor={descriptor.key!} bind:value={entry.key} />
                <div class="flex gap-2">
                    <div class="flex-1">
                        <SmartInput descriptor={descriptor.value} bind:value={entry.value} />
                    </div>
                    <button type="button" on:click={() => remove(i)} class="btn-secondary !text-danger">
                        Remove
                    </button>
                </div>
            </div>
        {/each}
        <button type="button" on:click={addEntry} class="btn-secondary">
            + Add Entry
        </button>
    {:else}
        {#each value as item, i (i)}
            <div class="flex gap-3 items-center">
                <div class="flex-1">
                    <SmartInput descriptor={descriptor.value} bind:value={value[i]} />
                </div>
                <button type="button" on:click={() => remove(i)} class="btn-secondary !text-danger">
                    Remove
                </button>
            </div>
        {/each}
        <button type="button" on:click={addItem} class="btn-secondary">
            + Add Item
        </button>
    {/if}
</div>