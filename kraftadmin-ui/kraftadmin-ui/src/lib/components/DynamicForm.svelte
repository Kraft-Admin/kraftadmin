<script lang="ts">
	import FieldRenderer from '../components/inputs/FieldRenderer.svelte';

	import type {
		ColumnType,
		KraftAdminColumn,
		LookupDescriptor,
		ResourceRow
	} from '../types/resources';

	export let columns: KraftAdminColumn[] = [];
	export let onSubmit: (data: Record<string, unknown>) => void;
	export let resourceName: string = '';
	export let submitLabel: string = 'Save';
	export let initialData: ResourceRow | null = null;
	export let externalErrors: Record<string, string[]> = {};

	let formData: Record<string, unknown> = {};
	let lastLoadedId: string | null = null;
	let fields: KraftAdminColumn[] = [];
	let lastColumnsJson = '';

	let sessionUploadedUrls = new Set<string>();
	let originalFileUrls = new Set<string>();

	const FILE_TYPES: ColumnType[] = ['IMAGE', 'VIDEO', 'FILE', 'AUDIO', 'DOCUMENT'];
	const HIDDEN_FIELDS = ['id', 'createdAt', 'updatedAt', 'deletedAt'];
	const WIDE_FIELDS: ColumnType[] = [
		'TEXTAREA', 'JSON', 'OBJECT', 'ARRAY', 'WYSIWYG',
		'RELATION', 'MULTI_RELATION', 'VIDEO', 'IMAGE', 'FILE', 'AUDIO', 'DOCUMENT'
	];

	$: {
		const json = JSON.stringify(columns);
		if (json !== lastColumnsJson) {
			lastColumnsJson = json;
			fields = columns.filter((column) => column.visible && !HIDDEN_FIELDS.includes(column.name));
		}
	}

	$: if (columns.length > 0) {
		const currentId = initialData?.id ?? 'new';
		if (currentId !== lastLoadedId) {
			formData = buildFormData(columns, initialData);
			lastLoadedId = currentId;
			originalFileUrls = new Set(collectFileUrls(formData));
			sessionUploadedUrls = new Set();
		}
	}

	$: if (initialData?.id) {
		submitLabel = 'Update';
	}

	// Backend wraps every @Embedded value as { data, summary } (EmbeddedResponse).
	// Unwrap one level so callers work with the flat field map underneath.
	function unwrapEmbedded(raw: unknown): Record<string, unknown> {
		if (raw && typeof raw === 'object' && !Array.isArray(raw) && 'data' in (raw as Record<string, unknown>)) {
			const data = (raw as Record<string, unknown>).data;
			return data && typeof data === 'object' && !Array.isArray(data)
				? (data as Record<string, unknown>)
				: {};
		}
		return raw && typeof raw === 'object' && !Array.isArray(raw)
			? (raw as Record<string, unknown>)
			: {};
	}

	function collectFileUrls(data: Record<string, unknown>): string[] {
		const urls: string[] = [];

		columns.forEach((column: KraftAdminColumn) => {
			if (FILE_TYPES.includes(column.type)) {
				const value = data[column.name];

				if (typeof value === 'string' && value) {
					urls.push(value);
				}
				if (Array.isArray(value)) {
					value
						.filter((item): item is string => typeof item === 'string' && Boolean(item))
						.forEach((url) => urls.push(url));
				}
			}

			if (column.type === 'OBJECT' && column.subColumns) {
				const objectValue = data[column.name];
				if (!objectValue || typeof objectValue !== 'object' || Array.isArray(objectValue)) return;

				const objectData = objectValue as Record<string, unknown>;

				column.subColumns.forEach((subColumn) => {
					// subColumn IS a KraftAdminColumn — type lives directly on it,
					// not under a nested `.value`.
					if (!FILE_TYPES.includes(subColumn.type)) return;

					const value = objectData[subColumn.name];

					if (typeof value === 'string' && value) {
						urls.push(value);
					}
					if (Array.isArray(value)) {
						value
							.filter((item): item is string => typeof item === 'string' && Boolean(item))
							.forEach((url) => urls.push(url));
					}
				});
			}
		});

		return urls;
	}

	async function deleteUrl(url: string): Promise<void> {
		try {
			await fetch('/admin/api/uploads', {
				method: 'DELETE',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ url })
			});
		} catch (error) {
			console.error('[DynamicForm] Failed to delete uploaded file:', url, error);
		}
	}

	async function handleCancel(): Promise<void> {
		const toDelete = [...sessionUploadedUrls].filter((url) => !originalFileUrls.has(url));
		if (toDelete.length > 0) {
			await Promise.all(toDelete.map(deleteUrl));
		}
		window.history.back();
	}

	function arrayToDatetimeLocal(arr: unknown[]): string {
		const [year, month, day, hour = 0, minute = 0] = arr as number[];
		return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}T${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
	}

	function arrayToDate(arr: unknown[]): string {
		const [year, month, day] = arr as number[];
		return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
	}

	function buildFormData(
		cols: KraftAdminColumn[],
		data: ResourceRow | null
	): Record<string, unknown> {
		const values = data?.values ?? {};
		const result: Record<string, unknown> = {};
		const rawId = data?.id;

		if (rawId !== undefined && rawId !== null && rawId !== '') {
			result.id = rawId;
		}

		cols.forEach((column) => {
			try {
				const raw = values[column.name] ?? null;

				switch (column.type) {
				
					case 'OBJECT': {
	result[column.name] = mapObjectValue(
		raw,
		column
	);
	break;
}

					case 'DATETIME':
						result[column.name] = Array.isArray(raw) ? arrayToDatetimeLocal(raw) : raw ?? '';
						break;

					case 'DATE':
						result[column.name] = Array.isArray(raw) ? arrayToDate(raw) : raw ?? '';
						break;

					case 'TIME':
						result[column.name] =
							typeof raw === 'string'
								? raw.includes('T')
									? raw.split('T')[1].substring(0, 5)
									: raw.substring(0, 5)
								: '';
						break;

					case 'RELATION':
						result[column.name] =
							raw && typeof raw === 'object' && !Array.isArray(raw)
								? (raw as { id?: unknown }).id ?? null
								: raw ?? null;
						break;

					case 'MULTI_RELATION':
						result[column.name] = Array.isArray(raw)
							? raw.map((item) =>
									item && typeof item === 'object' ? (item as { id?: unknown }).id ?? item : item
								)
							: [];
						break;

					case 'NUMBER':
					case 'RANGE':
						result[column.name] = typeof raw === 'number' ? raw : raw ?? 0;
						break;

					case 'CHECKBOX':
						result[column.name] = typeof raw === 'boolean' ? raw : false;
						break;

					case 'ARRAY':
					case 'MULTI_SELECT':
						result[column.name] = Array.isArray(raw)
							? raw
							: typeof raw === 'string' && raw
								? raw.split(',').map((item) => item.trim()).filter(Boolean)
								: [];
						break;

					case 'COLLECTION':
						if (raw == null) {
							result[column.name] = [];
						} else if (Array.isArray(raw)) {
							result[column.name] = raw;
						} else if (typeof raw === 'object') {
							result[column.name] = Object.entries(raw as Record<string, unknown>).map(
								([key, value]) => ({ key, value })
							);
						} else {
							result[column.name] = [raw];
						}
						break;

					default:
						if (raw !== null && typeof raw === 'object') {
							if (Array.isArray(raw)) {
								result[column.name] = raw.join(', ');
							} else {
								const objectValue = raw as Record<string, unknown>;
								result[column.name] = objectValue.displayField ?? objectValue.id ?? JSON.stringify(raw);
							}
						} else {
							result[column.name] = raw ?? column.defaultValue ?? '';
						}
				}
			} catch (error) {
				console.error(`[DynamicForm] Failed to map field "${column.name}" (${column.type}):`, error);
				result[column.name] = null;
			}
		});

		return result;
	}

	function handleFieldChange(columnName: string, value: unknown): void {
		formData = { ...formData, [columnName]: value };
	}

	function handleFileClear(columnName: string, index: number | null): void {
		const current = formData[columnName];

		if (typeof current === 'string') {
			sessionUploadedUrls.delete(current);
		} else if (Array.isArray(current) && index !== null) {
			const url = current[index];
			if (typeof url === 'string') {
				sessionUploadedUrls.delete(url);
			}
		}

		if (Array.isArray(current)) {
			formData = { ...formData, [columnName]: current.filter((_, i) => i !== index) };
		} else {
			formData = { ...formData, [columnName]: null };
		}
	}

	function handleFileUploaded(columnName: string, url: string): void {
		sessionUploadedUrls = new Set([...sessionUploadedUrls, url]);
	}

	function handleSubmit(): void {
		sessionUploadedUrls = new Set();
		onSubmit(formData);
	}

	function getLookup(fieldName: string): LookupDescriptor | undefined {
		return initialData?.relatedResources?.[fieldName]?.lookupDescriptor ?? undefined;
	}

	function mapObjectValue(
	raw: unknown,
	column: KraftAdminColumn
): Record<string, unknown> {
	const source = unwrapEmbedded(raw);
	const result: Record<string, unknown> = {};

	for (const subColumn of column.subColumns ?? []) {
		const subRaw = source[subColumn.name];

		if (subColumn.type === 'OBJECT') {
			result[subColumn.name] = mapObjectValue(
				subRaw,
				subColumn
			);
			continue;
		}

		if (
			Array.isArray(subRaw) &&
			subColumn.type === 'DATETIME'
		) {
			result[subColumn.name] =
				arrayToDatetimeLocal(subRaw);
			continue;
		}

		if (
			Array.isArray(subRaw) &&
			subColumn.type === 'DATE'
		) {
			result[subColumn.name] =
				arrayToDate(subRaw);
			continue;
		}

		if (subRaw !== null && subRaw !== undefined) {
			result[subColumn.name] = subRaw;
		} else {
			result[subColumn.name] =
				subColumn.defaultValue ?? '';
		}
	}

	return result;
}
</script>

<div class="form-container !p-0">
  <div class="grid grid-cols-1 md:grid-cols-2 gap-6 p-8">
    {#each fields as col (col.name)}
      {@const colErrors = externalErrors[col.name]}
      {@const isWide = WIDE_FIELDS.includes(col.type)}

      <div class="flex flex-col gap-1.5 {isWide ? 'md:col-span-2' : ''}">
        <label class="field-label">
          {col.label}
          {#if col.required}<span class="text-danger">*</span>{/if}
        </label>

        <FieldRenderer
          {col}
          value={formData[col.name]}
          error={colErrors}
          on:change={(e) => handleFieldChange(col.name, e.detail.value)}
          on:fileclear={(e) => handleFileClear(col.name, e.detail.index)}
          on:fileuploaded={(e) => handleFileUploaded(col.name, e.detail.url)}
          lookup={getLookup(col.name)}
        />

        {#if colErrors?.length}
          <div class="flex flex-col gap-0.5 mt-0.5">
            {#each colErrors as err}
              <span class="text-danger text-[10px] font-bold uppercase tracking-tight flex items-center gap-1">
                <svg class="w-3 h-3 flex-shrink-0" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clip-rule="evenodd"/>
                </svg>
                {err}
              </span>
            {/each}
          </div>
        {/if}
      </div>
    {/each}
  </div>

  <div class="form-footer px-8">
    <button type="button" on:click={handleCancel} class="btn-secondary">Cancel</button>
    <button type="button" on:click={handleSubmit} class="btn-primary">{submitLabel} {resourceName}</button>
  </div>
</div>