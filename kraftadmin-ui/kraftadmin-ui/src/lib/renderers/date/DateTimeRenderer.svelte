<script lang="ts">
    export let value: any;

    function formatDate(v: any) {
        if (!v) return null;
        if (Array.isArray(v)) {
            return `${v[0]}-${String(v[1]).padStart(2, '0')}-${String(v[2]).padStart(2, '0')}`;
        }
        return new Date(v).toISOString().split('T')[0];
    }

    function formatTime(v: any) {
        if (!v) return null;
        if (Array.isArray(v)) {
            let hour = v[3] || 0;
            const minute = String(v[4] || 0).padStart(2, '0');
            const ampm = hour >= 12 ? 'PM' : 'AM';
            hour = hour % 12 || 12; // Convert 0 to 12 for 12-hour format
            return `${hour}:${minute} ${ampm}`;
        }
        // Browser locale string handles AM/PM based on environment
        return new Date(v).toLocaleTimeString([], { 
            hour: '2-digit', 
            minute: '2-digit', 
            hour12: true 
        });
    }
</script>

{#if value}
    <div class="flex flex-col font-mono text-text-main leading-none">
        <span class="text-xs">{formatDate(value)}</span>
        <span class="text-[10px] text-text-muted mt-0.5">{formatTime(value)}</span>
    </div>
{:else}
    <span class="text-text-muted italic">—</span>
{/if}