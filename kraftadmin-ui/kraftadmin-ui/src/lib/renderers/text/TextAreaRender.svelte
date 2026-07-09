<script lang="ts">
    export let value: any;
</script>

{#if value != null && value !== ""}
<div class="whitespace-pre-wrap leading-relaxed text-text-main">
    {value}
</div>
{:else}
<span class="text-text-muted">—</span>
{/if}