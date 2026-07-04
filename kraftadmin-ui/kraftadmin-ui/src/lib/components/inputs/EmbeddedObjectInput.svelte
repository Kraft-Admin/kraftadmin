<script lang="ts">
  import { createEventDispatcher } from 'svelte';
  import TextInput from '../inputs/TextInput.svelte';
  import SelectInput from '../inputs/SelectInput.svelte';
  import CheckboxInput from '../inputs/CheckboxInput.svelte';
  import DateTimeInput from '../inputs/DateTimeInput.svelte';
  import ColorInput from '../inputs/ColorInput.svelte';
  import ArrayInput from '../inputs/ArrayInput.svelte';
  import FileUploader from './FileUploader.svelte';

  export let subColumns: any[] = [];
  export let value: Record<string, any> = {};
  export let label: string = '';

  const dispatch = createEventDispatcher();

  // ✅ Local copy — we never mutate `value` prop directly
  let local: Record<string, any> = {};

  // Sync from parent → local when value reference changes
  // (e.g. when DynamicForm initializes formData from scratch)
  $: {
    const incoming = value ?? {};
    // Only sync if the content actually changed — avoids wiping
    // local edits when parent re-renders for unrelated reasons
    if (JSON.stringify(incoming) !== JSON.stringify(local)) {
      local = { ...incoming };
    }
  }

  // ✅ Single update path — every input calls this, which dispatches
  // the full updated object up to DynamicForm via the 'change' event
  function update(fieldName: string, newVal: any) {
    local = { ...local, [fieldName]: newVal };
    dispatch('change', local); // DynamicForm receives the whole sub-object
  }

  // Stable fields — filter out system-managed subcolumns
  $: fields = subColumns.filter(
    c => c.visible !== false && !['id', 'createdAt', 'updatedAt'].includes(c.name)
  );
</script>

<div class="embedded-root">
  <header class="embedded-header">
    <span class="embedded-label">{label}</span>
    <div class="embedded-rule"></div>
  </header>

  <div class="embedded-grid">
    {#each fields as sub (sub.name)}
      <div class="embedded-field {['TEXTAREA','JSON','WYSIWYG','ARRAY','IMAGE','VIDEO','FILE','AUDIO','DOCUMENT'].includes(sub.type) ? 'span-2' : ''}">
        <label class="field-label">
          {sub.label ?? sub.name}
          {#if sub.required}<span class="req-star">*</span>{/if}
        </label>

        {#if sub.type === 'TEXT' || sub.type === 'EMAIL' || sub.type === 'URL' || sub.type === 'TEL' || sub.type === 'PASSWORD'}
          <TextInput
            value={local[sub.name] ?? ''}
            type={sub.type.toLowerCase()}
            placeholder={sub.placeholder ?? ''}
            on:change={(e) => update(sub.name, e.detail)}
          />

        {:else if sub.type === 'TEXTAREA' || sub.type === 'JSON' || sub.type === 'WYSIWYG' || sub.type === 'ARRAY'}
          <ArrayInput
            value={local[sub.name] ?? (sub.type === 'ARRAY' ? [] : '')}
            type={sub.type}
            on:change={(e) => update(sub.name, e.detail)}
          />

        {:else if sub.type === 'SELECT'}
          <SelectInput
            value={local[sub.name] ?? null}
            options={sub.selectOptions ?? []}
            placeholder={`Select ${sub.label ?? sub.name}...`}
            on:change={(e) => update(sub.name, e.detail)}
          />

        {:else if sub.type === 'MULTI_SELECT'}
          <SelectInput
            value={local[sub.name] ?? []}
            options={sub.selectOptions ?? []}
            multiple
            on:change={(e) => update(sub.name, e.detail)}
          />

        {:else if sub.type === 'CHECKBOX'}
          <CheckboxInput
            value={local[sub.name] ?? false}
            on:change={(e) => update(sub.name, e.detail)}
          />

        {:else if sub.type === 'DATETIME' || sub.type === 'DATE' || sub.type === 'TIME'}
          <DateTimeInput
            value={local[sub.name] ?? ''}
            type={sub.type}
            on:change={(e) => update(sub.name, e.detail)}
          />

        {:else if sub.type === 'NUMBER'}
          <input
            type="number"
            step="any"
            value={local[sub.name] ?? 0}
            class="input-base"
            on:input={(e) => update(sub.name, parseFloat(e.currentTarget.value))}
          />

        {:else if sub.type === 'RANGE'}
          <div class="flex flex-col gap-1">
            <input
              type="range"
              step="any"
              value={local[sub.name] ?? 0}
              class="range-input"
              on:input={(e) => update(sub.name, parseFloat(e.currentTarget.value))}
            />
            <span class="text-xs text-brand-primary font-mono">{local[sub.name] ?? 0}</span>
          </div>

        {:else if sub.type === 'COLOR'}
          <ColorInput
            value={local[sub.name] ?? '#000000'}
            on:change={(e) => update(sub.name, e.detail)}
          />

        {:else if sub.type === 'RADIO'}
          <div class="flex flex-wrap gap-4 py-1">
            {#each sub.selectOptions ?? [] as opt}
              <label class="flex items-center gap-2 cursor-pointer text-sm text-text-secondary">
                <input
                  type="radio"
                  value={opt.value}
                  checked={local[sub.name] === opt.value}
                  on:change={() => update(sub.name, opt.value)}
                  class="accent-brand"
                />
                {opt.label}
              </label>
            {/each}
          </div>

        {:else if ['IMAGE', 'VIDEO', 'FILE', 'AUDIO', 'DOCUMENT'].includes(sub.type)}
          <FileUploader
            col={sub}
            value={local[sub.name] ?? null}
            on:change={(e) => update(sub.name, e.detail.value)}
            on:clear={() => update(sub.name, null)}
          />

        {:else}
          <!-- Fallback: plain text -->
          <TextInput
            value={local[sub.name] ?? ''}
            placeholder={sub.placeholder ?? ''}
            on:change={(e) => update(sub.name, e.detail)}
          />
        {/if}
      </div>
    {/each}
  </div>
</div>

<style>
  .embedded-root {
    width: 100%;
  }

  .embedded-header {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    margin-bottom: 1rem;
  }

  .embedded-label {
    font-size: 0.7rem;
    font-weight: 800;
    color: var(--brand-primary, #3b82f6);
    text-transform: uppercase;
    letter-spacing: 0.08em;
    white-space: nowrap;
  }

  .embedded-rule {
    flex: 1;
    height: 1px;
    background: var(--border-subtle, #27272a);
  }

  .embedded-grid {
    display: grid;
    grid-template-columns: 1fr;
    gap: 1rem;
    padding: 1.25rem;
    background: var(--bg-main, #0c0c0e);
    border: 1px dashed var(--border-subtle, #27272a);
    border-radius: 0.5rem;
  }

  @media (min-width: 768px) {
    .embedded-grid {
      grid-template-columns: repeat(2, 1fr);
    }
    .span-2 {
      grid-column: span 2;
    }
  }

  .embedded-field {
    display: flex;
    flex-direction: column;
    gap: 0.375rem;
  }

  .field-label {
    font-size: 0.7rem;
    font-weight: 700;
    color: var(--text-muted, #52525b);
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  .req-star { color: #ef4444; }

  .range-input {
    width: 100%;
    accent-color: var(--brand-primary, #3b82f6);
    cursor: pointer;
  }
</style>