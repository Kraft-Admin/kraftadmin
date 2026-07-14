<script lang="ts">
    import CollectionInput from "./CollectionInput.svelte";
    import EmbeddableInput from "./EmbeddableInput.svelte";
    import EnumInput from "./EnumInput.svelte";
    import ScalarInput from "./ScalarInput.svelte";


    import {
        ValueType,
        type ValueDescriptor
    } from "../../types/descriptors";


    export let descriptor: ValueDescriptor;

            import { createEventDispatcher } from "svelte";


    const dispatch = createEventDispatcher();

export let value:any;

let previous = value;

$: if (previous !== value) {
    previous = value;
    dispatch("change", value);
}

</script>

{#if descriptor.collection}

    <CollectionInput
        descriptor={descriptor.collection}
        bind:value
    />

{:else if descriptor.type === ValueType.EMBEDDABLE}

    <EmbeddableInput
        descriptor={descriptor}
        bind:value
    />

{:else if descriptor.type === ValueType.ENUM}

    <EnumInput
        descriptor={descriptor}
        bind:value
    />

{:else}

    <ScalarInput
        descriptor={descriptor}
        bind:value
    />

{/if}
