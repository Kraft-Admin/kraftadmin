<script lang="ts">
    export let value: string | string[] | null = null;

    $: items = Array.isArray(value)
        ? value.filter(Boolean)
        : value
            ? [value]
            : [];
</script>

{#if items.length}

<div class="flex flex-col gap-3">
    {#each items as audio}
        <audio
            controls
            preload="metadata"
            class="w-full"
        >
            <source src={audio}>
        </audio>
    {/each}
</div>

{:else}

<span class="text-text-muted italic">—</span>

{/if}