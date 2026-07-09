<script lang="ts">
    export let value: number;
</script>

{#if value == null}
    <span class="text-text-muted">—</span>
{:else}
    <span class="font-mono text-text-main">
        {new Intl.NumberFormat().format(value)}
    </span>
{/if}