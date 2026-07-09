<script lang="ts">
    export let value: string;
</script>

<div class="prose max-w-none text-text-main prose-headings:text-text-main prose-strong:text-text-main prose-a:text-brand-primary">
    {@html value}
</div>

<!-- <script lang="ts">
    export let value: string = "";
    export let mode: "table" | "detail" = "detail";

    let expanded = false;

    $: hasContent = value.trim().length > 0;
    $: shouldCollapse =
        mode === "table" &&
        value.replace(/<[^>]+>/g, "").length > 300;
</script>

{#if hasContent}
    <div class="space-y-2">
        <div
            class:collapsed={mode === "table" && shouldCollapse && !expanded}
            class="wysiwyg-content"
        >
            {@html value}
        </div>

        {#if mode === "table" && shouldCollapse}
            <button
                type="button"
                class="text-xs font-semibold text-brand-primary hover:underline"
                on:click={() => expanded = !expanded}
            >
                {expanded ? "Show less" : "Read more"}
            </button>
        {/if}
    </div>
{:else}
    <span class="italic text-zinc-500">—</span>
{/if}

<style>
    .collapsed {
        max-height: 8rem;
        overflow: hidden;
    }

    .wysiwyg-content {
        line-height: 1.7;
    }

    .wysiwyg-content :global(h1) {
        font-size: 2rem;
        font-weight: 700;
        margin: 1rem 0;
    }

    .wysiwyg-content :global(h2) {
        font-size: 1.5rem;
        font-weight: 700;
        margin: 0.9rem 0;
    }

    .wysiwyg-content :global(h3) {
        font-size: 1.25rem;
        font-weight: 600;
        margin: 0.75rem 0;
    }

    .wysiwyg-content :global(p) {
        margin: 0.75rem 0;
    }

    .wysiwyg-content :global(ul) {
        list-style: disc;
        padding-left: 1.5rem;
        margin: 0.75rem 0;
    }

    .wysiwyg-content :global(ol) {
        list-style: decimal;
        padding-left: 1.5rem;
        margin: 0.75rem 0;
    }

    .wysiwyg-content :global(li) {
        margin: 0.25rem 0;
    }

    .wysiwyg-content :global(blockquote) {
        border-left: 4px solid rgb(82 82 91);
        padding-left: 1rem;
        font-style: italic;
        margin: 1rem 0;
    }

    .wysiwyg-content :global(pre) {
        overflow-x: auto;
        padding: 1rem;
        border-radius: 0.5rem;
        background: rgb(24 24 27);
    }

    .wysiwyg-content :global(code) {
        font-family: monospace;
    }

    .wysiwyg-content :global(table) {
        width: 100%;
        border-collapse: collapse;
        margin: 1rem 0;
    }

    .wysiwyg-content :global(th),
    .wysiwyg-content :global(td) {
        border: 1px solid rgb(63 63 70);
        padding: 0.5rem;
    }

    .wysiwyg-content :global(img) {
        max-width: 100%;
        height: auto;
        border-radius: 0.5rem;
    }

    .wysiwyg-content :global(a) {
        color: var(--brand-primary);
        text-decoration: underline;
    }
</style> -->