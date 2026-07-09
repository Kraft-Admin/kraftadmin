<script lang="ts">
    export let value: any;

    function format(v: any) {
        if (Array.isArray(v)) {
            return `${v[0]}-${String(v[1]).padStart(2, '0')}-${String(v[2]).padStart(2, '0')}`;
        }
        return new Date(v).toLocaleDateString();
    }
</script>

{#if value}
    <span class="font-mono text-text-main">
        {format(value)}
    </span>
{:else}
    <span class="text-text-muted italic">—</span>
{/if}