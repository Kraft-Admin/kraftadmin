<!-- <script lang="ts">
    import { createEventDispatcher } from "svelte";
    import type { ValueDescriptor } from "../../types/descriptors";

    export let descriptor: ValueDescriptor;
    export let value: any;

    const dispatch = createEventDispatcher();
    $: dispatch("change", value);
</script>

<select class="input-base" bind:value>
    <option value="">Select...</option>
    {#each descriptor.enumValues as option}
        <option value={option.value}>{option.label}</option>
    {/each}
</select> -->

<script lang="ts">
    import { createEventDispatcher } from "svelte";
    import type { ValueDescriptor } from "../../types/descriptors";

    export let descriptor: ValueDescriptor;
    export let value: any;

    const dispatch = createEventDispatcher();
    $: dispatch("change", value);
</script>

<select class="input-base text-text-main" bind:value>
    <option value="" disabled selected>Select...</option>
    {#each descriptor.enumValues as option}
        <option value={option.value} class="bg-bg-surface text-text-main">
            {option.label}
        </option>
    {/each}
</select>