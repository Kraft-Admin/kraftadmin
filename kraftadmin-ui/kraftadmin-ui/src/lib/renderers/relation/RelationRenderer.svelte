<!-- <script lang="ts">
    export let value: any;
</script>

{#if value}
    <div class="inline-flex items-center rounded-full bg-brand-primary/10 px-3 py-1 text-xs text-brand-primary font-medium">
        {value.displayField ?? value.id}
    </div>
{:else}
    <span class="text-text-muted italic">—</span>
{/if}
 -->


<script lang="ts">
    import { link } from "svelte-spa-router";

    export let value: any = null;
    export let relatedCollection: any = null;
    export let mode: "table" | "detail" = "table";

    // Normalize: Handle both direct value or the item from a RelatedCollection
    $: item = relatedCollection?.items?.[0] ?? value;
    $: entityType = relatedCollection?.entityType ?? value?.entityType ?? "unknown";
</script>

{#if !item || Object.keys(item).length === 0}
    <span class="text-text-muted italic">—</span>

{:else if mode === "table"}
    <a
        use:link
        href="/resources/{entityType}/{item.id}"
        class="inline-flex items-center gap-2 rounded-full bg-brand-primary/10 px-3 py-1 text-xs font-semibold text-brand-primary hover:bg-brand-primary/20 transition-colors"
    >
        <div class="h-1.5 w-1.5 rounded-full bg-brand-primary"></div>
        {item.displayLabel ?? item.displayField ?? item.id}
    </a>

{:else}
    <div class="card p-5 border border-border-subtle hover:border-brand-primary transition-colors">
        <div class="font-bold text-text-main text-lg">
            {item.displayLabel ?? item.displayField ?? item.id}
        </div>
        
        <div class="mt-1 text-[10px] font-mono text-text-muted uppercase tracking-wider">
            ID: {item.id.slice(0, 8)}
        </div>

        <div class="mt-5 flex gap-3">
            <a
                use:link
                href="/resources/{entityType}/{item.id}"
                class="btn-primary text-xs !px-3 !py-1.5"
            >
                View
            </a>
            <a
                use:link
                href="/resources/{entityType}"
                class="btn-secondary text-xs !px-3 !py-1.5"
            >
                Manage
            </a>
        </div>
    </div>
{/if}