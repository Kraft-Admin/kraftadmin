<script lang="ts">
    export let value: string | string[] | null = null;

    $: items = Array.isArray(value)
        ? value.filter(Boolean)
        : value
            ? [value]
            : [];
</script>

{#if items.length}

<div class="grid gap-4 md:grid-cols-2">
    {#each items as video}
        <video
            controls
            preload="metadata"
            class="w-full rounded-xl border border-border-subtle bg-black"
        >
            <source src={video}>
        </video>
    {/each}
</div>

{:else}

<span class="text-text-muted italic">—</span>

{/if}