<script lang="ts">
    export let value: string | string[] | null = null;

    $: items = Array.isArray(value)
        ? value.filter(Boolean)
        : value
            ? [value]
            : [];

    function filename(url: string) {
        return url.split("/").pop() ?? "Document";
    }
</script>

{#if items.length}

<div class="flex flex-col gap-3">

    {#each items as document}

        <a
            href={document}
            target="_blank"
            rel="noopener noreferrer"
            class="flex items-center gap-3 p-3 rounded-lg border border-border-subtle bg-bg-surface hover:border-brand-primary transition"
        >
            <span class="text-2xl">📄</span>

            <div class="flex-1 min-w-0">
                <div class="font-medium truncate">
                    {filename(document)}
                </div>

                <div class="text-xs text-text-muted">
                    Open document
                </div>
            </div>

        </a>

    {/each}

</div>

{:else}

<span class="text-text-muted italic">—</span>

{/if}