<script lang="ts">
    export let value: boolean;
</script>

<div class="flex items-center gap-2">
    <div class="w-2 h-2 rounded-full {value ? 'bg-success' : 'bg-text-muted'}"></div>

    <span class="text-xs uppercase font-bold {value ? 'text-success' : 'text-text-muted'}">
        {value ? "True" : "False"}
    </span>
</div>