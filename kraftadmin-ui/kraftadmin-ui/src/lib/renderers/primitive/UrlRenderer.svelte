<script lang="ts">
    export let value: string;
</script>

<a
    href={value}
    target="_blank"
    rel="noopener noreferrer"
    class="text-brand-primary underline transition-colors hover:opacity-80">
    {value}
</a>