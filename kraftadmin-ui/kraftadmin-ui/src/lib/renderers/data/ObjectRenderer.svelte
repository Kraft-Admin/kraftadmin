<script lang="ts">
    export let value: any;
</script>

{#if value != null}
    <div class="rounded-lg border border-border-subtle bg-bg-main p-3 overflow-auto max-h-[400px]">
        <pre class="text-xs font-mono text-text-main">
{JSON.stringify(value, null, 2)}
        </pre>
    </div>
{:else}
    <span class="text-text-muted italic">—</span>
{/if}