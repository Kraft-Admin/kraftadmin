<script lang="ts">
  import { createEventDispatcher } from 'svelte';
  export let value: string = '';
  export let type: 'DATETIME' | 'DATE' | 'TIME' = 'DATETIME';
  const dispatch = createEventDispatcher();

  const inputType = type === 'DATETIME' ? 'datetime-local' : type.toLowerCase();
</script>
<input
  type={inputType}
  value={value ?? ''}
  class="input-base"
  on:change={(e) => dispatch('change', e.currentTarget.value)}
/>