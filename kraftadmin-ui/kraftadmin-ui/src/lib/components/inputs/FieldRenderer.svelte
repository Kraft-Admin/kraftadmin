<script lang="ts">
  import SelectInput from './SelectInput.svelte';
  import RelationInput from './RelationInput.svelte';
  import MultiRelationInput from './MultiRelationInput.svelte';
  import CheckboxInput from './CheckboxInput.svelte';
  import DateTimeInput from './DateTimeInput.svelte';
  import ArrayInput from './ArrayInput.svelte';
  import ColorInput from './ColorInput.svelte';
  import EmbeddedObjectInput from './EmbeddedObjectInput.svelte';
  import FileUploader from './FileUploader.svelte';
  import WYSIWYG from './WYSIWYG.svelte';

  export let col: any;
  export let value: any;
  export let error: string[] | undefined = undefined;
  export let lookup: any = undefined;

  import { createEventDispatcher } from 'svelte';
  import CollectionInput from './CollectionInput.svelte';
  const dispatch = createEventDispatcher();

  function onChange(newVal: any) {
    dispatch('change', { field: col.name, value: newVal });
  }

  // Helper to map FormInputType enum strings to HTML input types
  const getNativeInputType = (type: string) => {
    switch (type) {
      case 'EMAIL': return 'email';
      case 'PASSWORD': return 'password';
      case 'TEL': return 'tel';
      case 'URL': return 'url';
      case 'SEARCH': return 'search';
      case 'HIDDEN': return 'hidden';
      default: return 'text';
    }
  };
</script>

{#if col.type === 'CHECKBOX'}
  <CheckboxInput {value} on:change={(e) => onChange(e.detail)} />

{:else if col.type === 'SELECT'}
  <SelectInput {value} options={col.selectOptions} placeholder={`Select ${col.label}...`} on:change={(e) => onChange(e.detail)} />

{:else if col.type === 'MULTI_SELECT'}
  <SelectInput {value} options={col.selectOptions} multiple on:change={(e) => onChange(e.detail)} />

{:else if col.type === 'RELATION'}
  <RelationInput {value}   lookup={col.lookup} on:change={(e) => onChange(e.detail)} />

{:else if col.type === 'MULTI_RELATION'}
  <MultiRelationInput {value} lookup={col.lookup} on:change={(e) => onChange(e.detail)} />

{:else if ['DATETIME', 'DATE', 'TIME'].includes(col.type)}
  <DateTimeInput {value} type={col.type} on:change={(e) => onChange(e.detail)} />

{:else if col.type === 'NUMBER' || col.type === 'RANGE'}
  <input
    type={col.type === 'RANGE' ? 'range' : 'number'}
    value={value ?? 0}
    class="input-base"
    on:input={(e) => onChange(parseFloat(e.currentTarget.value))}
  />

{:else if col.type === 'COLOR'}
  <ColorInput {value} on:change={(e) => onChange(e.detail)} />

{:else if ['ARRAY', 'TEXTAREA', 'JSON'].includes(col.type)}
  <ArrayInput {value} type={col.type} on:change={(e) => onChange(e.detail)} />

{:else if col.type === 'WYSIWYG'}
  <WYSIWYG value={value} config={col.wysiwygConfig} onChange={(html) => onChange(html)} />


  {:else if col.type === 'COLLECTION'}
    <CollectionInput
        descriptor={col.elementCollection}
        value={value} 
        on:change={(e) => onChange(e.detail)} 
    />


{:else if col.type === 'OBJECT'}
  <EmbeddedObjectInput
    label={col.label}
    subColumns={col.subColumns ?? []}
    value={value ?? {}}
    on:change={(e) => dispatch('change', { field: col.name, value: e.detail })}
  />

{:else if ['IMAGE', 'VIDEO', 'FILE', 'AUDIO', 'DOCUMENT'].includes(col.type)}
  <FileUploader
    {col}
    {value}
    on:change={(e) => dispatch('change', { field: col.name, value: e.detail.value })}
    on:clear={(e) => dispatch('fileclear', { field: col.name, index: e.detail.index })}
    on:uploaded={(e) => dispatch('fileuploaded', { field: col.name, url: e.detail.url })}
  />

{:else}
  <input
    type={getNativeInputType(col.type)}
    value={value ?? ''}
    class="input-base {error ? 'input-error' : ''}"
    placeholder={col.type === 'HIDDEN' ? '' : `Enter ${col.label}...`}
    on:input={(e) => onChange(e.currentTarget.value)}
  />
{/if}