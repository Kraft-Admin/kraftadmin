<script lang="ts">
  export let subColumns: any[] = [];
  export let value: any = {};
  export let label = "";

  // ✅ Keyed fields — only recomputes when subColumns changes
  let fields: any[] = [];
  let lastJson = '';
  $: {
    const json = JSON.stringify(subColumns);
    if (json !== lastJson) {
      lastJson = json;
      fields = subColumns.filter(c => c.visible && !['id', 'createdAt', 'updatedAt'].includes(c.name));
    }
  }

  // ✅ Keep local formData in sync with parent value
  // Use a local copy to avoid mutating the parent object directly on every keystroke
  let formData: any = {};
  $: {
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      formData = { ...value };
    }
  }

  // ✅ Propagate local changes back up to parent
  function update(fieldName: string, newVal: any) {
    formData = { ...formData, [fieldName]: newVal };
    value = formData;
  }
</script>

<div class="embedded-container">
  <header class="embedded-header">
    <span class="label-text">{label}</span>
    <div class="header-line"></div>
  </header>

  <div class="form-grid">
    {#each fields as col (col.name)}
      <div class="field-group {['TEXTAREA', 'JSON', 'WYSIWYG', 'ARRAY', 'VIDEO'].includes(col.type) ? 'span-2' : ''}">
        <label for={col.name} class="field-label">
          {col.label}
          {#if col.required}<span class="req-star">*</span>{/if}
        </label>

        {#if col.type === 'CHECKBOX'}
          <div class="toggle-wrapper">
            <button type="button"
              on:click={() => update(col.name, !formData[col.name])}
              class="toggle-btn" class:active={formData[col.name]}>
              <div class="toggle-dot" class:dot-active={formData[col.name]}></div>
            </button>
          </div>

        {:else if col.type === 'RADIO'}
          <div class="flex-row gap-4 py-1">
            {#each col.selectOptions || [] as opt}
              <label class="radio-label">
                <input type="radio"
                  checked={formData[col.name] === opt.value}
                  on:change={() => update(col.name, opt.value)} />
                <span>{opt.label}</span>
              </label>
            {/each}
          </div>

        {:else if col.type === 'SELECT'}
          <select
            class="input-base"
            value={formData[col.name] ?? null}
            on:change={(e) => update(col.name, e.currentTarget.value)}>
            <option value={null}>{col.placeholder || `Select ${col.label}...`}</option>
            {#each col.selectOptions || [] as opt}
              <option value={opt.value}>{opt.label}</option>
            {/each}
          </select>

        {:else if col.type === 'DATE'}
          <input type="date"
            class="input-base"
            value={formData[col.name] ?? ''}
            on:change={(e) => update(col.name, e.currentTarget.value)} />

        {:else if col.type === 'DATETIME'}
          <input type="datetime-local"
            class="input-base"
            value={formData[col.name] ?? ''}
            on:change={(e) => update(col.name, e.currentTarget.value)} />

        {:else if col.type === 'TIME'}
          <input type="time"
            class="input-base"
            value={formData[col.name] ?? ''}
            on:change={(e) => update(col.name, e.currentTarget.value)} />

        {:else if col.type === 'NUMBER' || col.type === 'RANGE'}
          <div class="flex flex-col gap-2">
            <input
              type={col.type === 'RANGE' ? 'range' : 'number'}
              step="any"
              class={col.type === 'RANGE' ? 'range-input' : 'input-base'}
              value={formData[col.name] ?? 0}
              on:input={(e) => update(col.name, parseFloat(e.currentTarget.value))} />
            {#if col.type === 'RANGE'}
              <span class="text-xs text-brand-primary font-mono">{formData[col.name] || 0}</span>
            {/if}
          </div>

        {:else if col.type === 'COLOR'}
          <div class="color-input-group">
            <input type="color"
              class="color-swatch"
              value={formData[col.name] ?? '#000000'}
              on:input={(e) => update(col.name, e.currentTarget.value)} />
            <input type="text"
              class="input-base mono"
              placeholder="#000000"
              value={formData[col.name] ?? ''}
              on:input={(e) => update(col.name, e.currentTarget.value)} />
          </div>

        {:else if col.type === 'ARRAY'}
          <input type="text"
            class="input-base"
            placeholder="e.g. Kotlin, Java"
            value={Array.isArray(formData[col.name]) ? formData[col.name].join(', ') : (formData[col.name] ?? '')}
            on:input={(e) => update(col.name, e.currentTarget.value.split(',').map((s: string) => s.trim()).filter(Boolean))} />

        {:else if col.type === 'JSON' || col.type === 'TEXTAREA' || col.type === 'WYSIWYG'}
          <textarea
            class="input-base mono"
            rows={4}
            placeholder={col.type === 'JSON' ? '{"key": "value"}' : '...'}
            value={formData[col.name] ?? ''}
            on:input={(e) => update(col.name, e.currentTarget.value)}></textarea>

        {:else if col.type === 'IMAGE'}
          <div class="image-field">
            {#if formData[col.name]}
              <div class="preview-box">
                <img src={formData[col.name]} alt="Preview" />
              </div>
            {/if}
            <input type="text"
              class="input-base"
              placeholder="Image URL..."
              value={formData[col.name] ?? ''}
              on:input={(e) => update(col.name, e.currentTarget.value)} />
          </div>

        {:else}
          <!-- TEXT, EMAIL, URL, TEL, PASSWORD — all safe string types -->
          <input
            type={['EMAIL', 'URL', 'NUMBER', 'TEL', 'PASSWORD'].includes(col.type) ? col.type.toLowerCase() : 'text'}
            placeholder={col.placeholder || ''}
            class="input-base"
            value={typeof formData[col.name] === 'string' ? formData[col.name] : (formData[col.name] ?? '')}
            on:input={(e) => update(col.name, e.currentTarget.value)} />
        {/if}
      </div>
    {/each}
  </div>
</div>
