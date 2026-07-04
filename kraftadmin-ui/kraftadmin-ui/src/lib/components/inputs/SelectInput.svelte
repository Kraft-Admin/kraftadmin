<script lang="ts">
  import { createEventDispatcher } from 'svelte';
  export let value: any = null;
  export let options: { value: any; label: string }[] = [];
  export let placeholder: string = 'Select...';
  export let multiple: boolean = false;
  export let error: boolean = false;
  const dispatch = createEventDispatcher();
</script>

{#if multiple}
  <select
    multiple
    value={value || []}
    class="input-base {error ? 'input-error' : ''}"
    on:change={(e) => {
      const selected = Array.from(e.currentTarget.selectedOptions).map(o => o.value);
      dispatch('change', selected);
    }}
  >
    {#each options as opt}
      <option value={opt.value}>{opt.label}</option>
    {/each}
  </select>
{:else}
  <select
    value={value}
    class="input-base {error ? 'input-error' : ''}"
    on:change={(e) => dispatch('change', e.currentTarget.value)}
  >
    <option value={null}>{placeholder}</option>
    {#each options as opt}
      <option value={opt.value}>{opt.label}</option>
    {/each}
  </select>
{/if}