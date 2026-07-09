<script lang="ts">
    export let value: string | string[] | null = null;

    $: items = Array.isArray(value)
        ? value.filter(Boolean)
        : value
            ? [value]
            : [];

    function filename(url: string) {
        return url.split("/").pop() ?? "File";
    }
</script>

{#if items.length}

<div class="flex flex-col gap-3">

    {#each items as file}

        <a
            href={file}
            target="_blank"
            rel="noopener noreferrer"
            class="flex items-center gap-3 p-3 rounded-lg border border-border-subtle bg-bg-surface hover:border-brand-primary transition"
        >
            <span class="text-2xl">📦</span>

            <div class="flex-1 min-w-0">
                <div class="font-medium truncate">
                    {filename(file)}
                </div>

                <div class="text-xs text-text-muted">
                    Download file
                </div>
            </div>

        </a>

    {/each}

</div>

{:else}

<span class="text-text-muted italic">—</span>

{/if}