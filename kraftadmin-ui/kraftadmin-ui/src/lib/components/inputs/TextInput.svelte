<script lang="ts">
  import { createEventDispatcher } from 'svelte';
  export let value: string = '';
  export let type: string = 'text';
  export let placeholder: string = '';
  export let error: boolean = false;
  const dispatch = createEventDispatcher();
</script>

<div class="flex flex-col gap-1">
  <input
    {type}
    {value}
    {placeholder}
    class="input-base {error ? 'input-error' : ''}"
    on:input={(e) => dispatch('change', e.currentTarget.value)}
  />
  {#if error}
  {/if}
</div>