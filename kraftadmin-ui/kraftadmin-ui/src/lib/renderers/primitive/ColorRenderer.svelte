<script lang="ts">
    export let value: string;
</script>

<div class="flex items-center gap-3">
    <div
        class="w-5 h-5 rounded border border-border-subtle"
        style={`background:${value}`}>
    </div>

    <span class="font-mono text-text-main">
        {value}
    </span>
</div>