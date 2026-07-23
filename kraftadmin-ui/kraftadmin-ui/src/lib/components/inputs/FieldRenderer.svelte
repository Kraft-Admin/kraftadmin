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
	import CollectionInput from './CollectionInput.svelte';

	import { createEventDispatcher } from 'svelte';

	import type {
		ColumnType,
		KraftAdminColumn,
		LookupDescriptor
	} from '../../types/resources';

	export let col: KraftAdminColumn;
	export let value: unknown;
	export let error: string[] | undefined = undefined;
	export let lookup: LookupDescriptor | undefined = undefined;

	const dispatch = createEventDispatcher();

	function onChange(newValue: unknown) {
		dispatch('change', {
			field: col.name,
			value: newValue
		});
	}

	function getNativeInputType(
		type: ColumnType
	): 'text' | 'email' | 'password' | 'tel' | 'url' | 'search' | 'hidden' {
		switch (type) {
			case 'EMAIL':
				return 'email';

			case 'PASSWORD':
				return 'password';

			case 'TEL':
				return 'tel';

			case 'URL':
				return 'url';

			case 'SEARCH':
				return 'search';

			case 'HIDDEN':
				return 'hidden';

			default:
				return 'text';
		}
	}
</script>

{#if col.type === 'CHECKBOX'}

	<CheckboxInput
	value={typeof value === 'boolean' ? value : false}
	on:change={(event) => onChange(event.detail)}
  />

{:else if col.type === 'SELECT'}

	<SelectInput
		{value}
		options={col.selectOptions ?? []}
		placeholder={col.placeholder}
		on:change={(event) => onChange(event.detail)}
	/>

{:else if col.type === 'MULTI_SELECT'}

	<SelectInput
		{value}
		options={col.selectOptions ?? []}
		multiple
		on:change={(event) => onChange(event.detail)}
	/>

{:else if col.type === 'RELATION'}

	<RelationInput
		value = {typeof value === 'string' ? value : null}
		lookup={col.lookup ?? lookup}
		on:change={(event) => onChange(event.detail)}
	/>

{:else if col.type === 'MULTI_RELATION'}

	<MultiRelationInput
	value={Array.isArray(value) ? value.filter((item): item is string => typeof item === 'string') : []}
	lookup={col.lookup ?? lookup}
	on:change={(event) => onChange(event.detail)}
/>

{:else if col.type === 'DATETIME' || col.type === 'DATE' || col.type === 'TIME'}

	<DateTimeInput
	value={typeof value === 'string' ? value : ''}
	type={col.type}
	on:change={(event) => onChange(event.detail)}
/>

{:else if col.type === 'NUMBER' || col.type === 'RANGE'}

	<input
		type={col.type === 'RANGE' ? 'range' : 'number'}
		value={typeof value === 'number' ? value : 0}
		class="input-base"
		on:input={(event) => {
			const target = event.currentTarget as HTMLInputElement;
			onChange(parseFloat(target.value));
		}}
	/>

{:else if col.type === 'COLOR'}

<ColorInput
	value={typeof value === 'string' ? value : '#000000'}
	on:change={(event) => onChange(event.detail)}
/>

{:else if col.type === 'ARRAY' || col.type === 'TEXTAREA' || col.type === 'JSON'}

	<ArrayInput
		{value}
		type={col.type}
		on:change={(event) => onChange(event.detail)}
	/>

{:else if col.type === 'WYSIWYG'}

	<WYSIWYG
		value={typeof value === 'string' ? value : ''}
		config={col.wysiwygConfig}
		onChange={(html) => onChange(html)}
	/>

{:else if col.type === 'COLLECTION' && col.elementCollection}

	<CollectionInput
		descriptor={col.elementCollection}
		value={Array.isArray(value) ? value : []}
		on:change={(event) => onChange(event.detail)}
	/>


{:else if col.type === 'OBJECT'}

	<EmbeddedObjectInput
		label={col.label}
		subColumns={col.subColumns ?? []}
		value={
			value &&
			typeof value === 'object' &&
			!Array.isArray(value)
				? value
				: {}
		}
		on:change={(event) => {
			dispatch('change', {
				field: col.name,
				value: event.detail
			});
		}}
	/>

{:else if col.type === 'IMAGE'
	|| col.type === 'VIDEO'
	|| col.type === 'FILE'
	|| col.type === 'AUDIO'
	|| col.type === 'DOCUMENT'}

	<FileUploader
		{col}
		{value}
		on:change={(event) => {
			dispatch('change', {
				field: col.name,
				value: event.detail.value
			});
		}}
		on:clear={(event) => {
			dispatch('fileclear', {
				field: col.name,
				index: event.detail.index
			});
		}}
		on:uploaded={(event) => {
			dispatch('fileuploaded', {
				field: col.name,
				url: event.detail.url
			});
		}}
	/>

{:else}

	<input
		type={getNativeInputType(col.type)}
		value={typeof value === 'string' ? value : ''}
		class:error={error && error.length > 0}
		class="input-base"
		placeholder={
			col.placeholder
		}
		on:input={(event) => {
			const target = event.currentTarget as HTMLInputElement;
			onChange(target.value);
		}}
	/>

{/if}