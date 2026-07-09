<script lang="ts">
    export let value: any;
</script>

{#if value}
    <span class="font-mono text-text-main">
        {value}
    </span>
{:else}
    <span class="text-text-muted italic">—</span>
{/if}