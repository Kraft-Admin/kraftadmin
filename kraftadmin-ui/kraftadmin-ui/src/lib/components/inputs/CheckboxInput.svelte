<script lang="ts">
  import { createEventDispatcher } from 'svelte';
  export let value: boolean = false;
  const dispatch = createEventDispatcher();
</script>
<button
  type="button" aria-label="checkbox"
  class="w-11 h-6 rounded-full relative transition-colors {value ? 'bg-brand-primary' : 'bg-border-subtle'}"
  on:click={() => dispatch('change', !value)}
>
  <div class="absolute top-1 left-1 bg-bg-surface w-4 h-4 rounded-full transition-transform {value ? 'translate-x-5' : ''}"></div>
</button>