<script lang="ts">
  import { createEventDispatcher } from 'svelte';
  export let value: string = '#000000';
  const dispatch = createEventDispatcher();
</script>
<div class="flex gap-2">
  <input type="color" value={value} class="h-10 w-10 rounded-lg border border-border-subtle cursor-pointer" on:input={(e) => dispatch('change', e.currentTarget.value)} />
  <input type="text" value={value} class="input-base font-mono" on:input={(e) => dispatch('change', e.currentTarget.value)} />
</div>