<script lang="ts">
	import TextRenderer from './text/TextRenderer.svelte';
	import TextAreaRenderer from './text/TextAreaRender.svelte';
	import ImageRenderer from './media/ImageRenderer.svelte';
	import NumberRenderer from './primitive/NumberRenderer.svelte';
	import DateRenderer from './date/DateRenderer.svelte';
	import DateTimeRenderer from './date/DateTimeRenderer.svelte';
	import TimeRenderer from './date/TimeRenderer.svelte';
	import VideoRenderer from './media/VideoRenderer.svelte';
	import AudioRenderer from './media/AudioRenderer.svelte';
	import FileRenderer from './media/FileRenderer.svelte';
	import DocumentRenderer from './media/DocumentRenderer.svelte';
	import RelationRenderer from './relation/RelationRenderer.svelte';
	import MultiRelationRenderer from './relation/MultiRelationRenderer.svelte';
	import BooleanRenderer from './primitive/BooleanRenderer.svelte';
	import UrlRenderer from './primitive/UrlRenderer.svelte';
	import EmailRenderer from './primitive/EmailRenderer.svelte';
	import ColorRenderer from './primitive/ColorRenderer.svelte';
	import WysiwygRenderer from './text/WysiwygRenderer.svelte';
	import JsonRenderer from './data/JsonRenderer.svelte';
	import SmartValueRenderer from './data/SmartValueRenderer.svelte';

	import type {
		ColumnType,
		ElementCollectionDescriptor,
		RelatedCollection
	} from '../types/resources';

	export let type: ColumnType;
	export let value: unknown;
	export let label: string;
	export let mode: 'table' | 'detail' = 'detail';
	export let relatedCollection: RelatedCollection | null = null;
	export let elementCollection: ElementCollectionDescriptor | null = null;

	// Dynamic renderers intentionally share a loose prop contract.
	// Individual renderer components may use only the props they need.
	type RendererComponent = any;

	const FILE_RENDERERS: Partial<Record<ColumnType, RendererComponent>> = {
		IMAGE: ImageRenderer,
		VIDEO: VideoRenderer,
		AUDIO: AudioRenderer,
		FILE: FileRenderer,
		DOCUMENT: DocumentRenderer
	};

	const registry: Partial<Record<ColumnType, RendererComponent>> = {
		TEXT: TextRenderer,
		TEXTAREA: TextAreaRenderer,
		NUMBER: NumberRenderer,
		DATE: DateRenderer,
		DATETIME: DateTimeRenderer,
		TIME: TimeRenderer,
		IMAGE: ImageRenderer,
		VIDEO: VideoRenderer,
		AUDIO: AudioRenderer,
		FILE: FileRenderer,
		DOCUMENT: DocumentRenderer,
		RELATION: RelationRenderer,
		MULTI_RELATION: MultiRelationRenderer,
		CHECKBOX: BooleanRenderer,
		URL: UrlRenderer,
		EMAIL: EmailRenderer,
		COLOR: ColorRenderer,
		WYSIWYG: WysiwygRenderer,
		SELECT: TextRenderer
	};

	let fileValueInputType: ColumnType | null = null;
	let CurrentRenderer: RendererComponent = null;

	$: {
		const inputType = elementCollection?.value?.inputType;

		fileValueInputType =
			type === 'COLLECTION' &&
			inputType &&
			inputType in FILE_RENDERERS
				? inputType as ColumnType
				: null;

		CurrentRenderer =
			(fileValueInputType
				? FILE_RENDERERS[fileValueInputType]
				: null) ||
			registry[type] ||
			null;
	}
</script>

<div class="renderer-wrapper">
	{#if CurrentRenderer}
	

        <svelte:component this={CurrentRenderer} {value} {relatedCollection} {label} {mode} />


	{:else if type === 'JSON'}
		<JsonRenderer {value} />

	{:else}
		<SmartValueRenderer
			{value}
			{label}
		/>
	{/if}
</div>