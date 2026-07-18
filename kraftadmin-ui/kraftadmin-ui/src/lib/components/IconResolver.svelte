<script lang="ts">
  import * as Icons from 'lucide-svelte';

  export let name: string; // The icon string: "airplay", "a-large-small", etc.
  export let fallback: string = 'Folder';

  // Helper to convert "a-large-small" to "ALargeSmall"
  function toPascalCase(str: string): string {
    return str
      .replace(/(^\w|-\w)/g, (match) => match.replace('-', '').toUpperCase());
  }

  $: iconKey = toPascalCase(name || fallback) + 'Icon';
  // Cast Icons to any to allow dynamic property access
  $: SelectedIcon = (Icons as any)[iconKey] || Icons.FolderIcon;
</script>

<svelte:component this={SelectedIcon} size={16} strokeWidth={2.5} />