<!-- <script lang="ts">

    import {
        ValueType,
        type ValueDescriptor
    } from "../../types/descriptors";

    export let descriptor:ValueDescriptor;


           import { createEventDispatcher } from "svelte";


    const dispatch = createEventDispatcher();

export let value:any;

$: dispatch("change", value);

</script>

{#if descriptor.type===ValueType.STRING || descriptor.type===ValueType.UUID}

    <input
        class="input"
        type="text"
        bind:value
    />

{:else if descriptor.type===ValueType.INT || descriptor.type===ValueType.LONG}

    <input
        class="input"
        type="number"
        step="1"
        bind:value
    />

{:else if descriptor.type===ValueType.DOUBLE}

    <input
        class="input"
        type="number"
        step="any"
        bind:value
    />

{:else if descriptor.type===ValueType.BOOLEAN}

    <input
        type="checkbox"
        bind:checked={value}
    />

{:else if descriptor.type===ValueType.DATE}

    <input
        class="input"
        type="date"
        bind:value
    />

{:else if descriptor.type===ValueType.DATETIME}

    <input
        class="input"
        type="datetime-local"
        bind:value
    />

{/if} -->

<script lang="ts">
    import {
        ValueType,
        type ValueDescriptor
    } from "../../types/resources";
    import { createEventDispatcher } from "svelte";

    export let descriptor: ValueDescriptor;
    export let value: any;

    const dispatch = createEventDispatcher();

    $: dispatch("change", value);
</script>

{#if descriptor.type === ValueType.STRING || descriptor.type === ValueType.UUID}
    <input
        class="input-base"
        type="text"
        bind:value
    />

{:else if descriptor.type === ValueType.INT || descriptor.type === ValueType.LONG}
    <input
        class="input-base"
        type="number"
        step="1"
        bind:value
    />

{:else if descriptor.type === ValueType.DOUBLE}
    <input
        class="input-base"
        type="number"
        step="any"
        bind:value
    />

{:else if descriptor.type === ValueType.BOOLEAN}
    <input
        class="size-5 rounded border-border-subtle bg-bg-surface text-brand-primary focus:ring-brand-primary"
        type="checkbox"
        bind:checked={value}
    />

{:else if descriptor.type === ValueType.DATE}
    <input
        class="input-base"
        type="date"
        bind:value
    />

{:else if descriptor.type === ValueType.DATETIME}
    <input
        class="input-base"
        type="datetime-local"
        bind:value
    />
{/if}