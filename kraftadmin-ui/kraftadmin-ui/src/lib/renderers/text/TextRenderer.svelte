<script lang="ts">
    export let value: unknown;
    export let mode: "table" | "detail" = "detail";
</script>

{#if value == null || value === ""}
    <span class="text-text-muted italic">—</span>
{:else if mode === "table"}
    <span class="truncate block max-w-[220px] text-text-main">
        {value}
    </span>
{:else}
    <div class="whitespace-pre-wrap text-text-main">
        {value}
    </div>
{/if}