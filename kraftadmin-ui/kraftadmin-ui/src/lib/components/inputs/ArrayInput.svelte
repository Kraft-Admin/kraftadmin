<script lang="ts">
  import { createEventDispatcher } from 'svelte';
  export let value: any = '';
  export let type: 'ARRAY' | 'TEXTAREA' | 'JSON' = 'TEXTAREA';
  const dispatch = createEventDispatcher();

  // ARRAY type: stored as string[], displayed as comma-separated
  $: displayValue = type === 'ARRAY' && Array.isArray(value) ? value.join(', ') : (value ?? '');

  function onInput(e: Event) {
    const raw = (e.target as HTMLTextAreaElement).value;
    dispatch('change', type === 'ARRAY' ? raw.split(',').map(s => s.trim()).filter(Boolean) : raw);
  }
</script>
<textarea
  value={displayValue}
  rows={type === 'ARRAY' ? 2 : 6}
  class="input-base !h-auto font-mono text-sm"
  placeholder={type === 'JSON' ? '{"key": "value"}' : type === 'ARRAY' ? 'item1, item2, item3' : ''}
  on:input={onInput}
></textarea>