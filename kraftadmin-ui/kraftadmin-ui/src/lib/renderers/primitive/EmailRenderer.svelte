<script lang="ts">
    export let value: string;
</script>

<a
    href={`mailto:${value}`}
    class="text-brand-primary underline transition-colors hover:opacity-80">
    {value}
</a>