<script lang="ts">
    export let value: any[] = [];
</script>

{#if value && value.length > 0}
    <div class="flex flex-wrap gap-2">
        {#each value as item}
            <span class="px-2 py-1 rounded text-xs font-medium bg-bg-main text-text-main border border-border-subtle">
                {item}
            </span>
        {/each}
    </div>
{:else}
    <span class="text-text-muted italic">—</span>
{/if}