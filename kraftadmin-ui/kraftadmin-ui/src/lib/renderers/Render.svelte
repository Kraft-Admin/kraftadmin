<script lang="ts">
  // Import your specialized renderers
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
  import ObjectRenderer from './data/ObjectRenderer.svelte';
  import ArrayRenderer from './data/ArrayRenderer.svelte';
  import JsonRenderer from './data/JsonRenderer.svelte';
  import BooleanRenderer from './primitive/BooleanRenderer.svelte';
  import UrlRenderer from './primitive/UrlRenderer.svelte';
  import EmailRenderer from './primitive/EmailRenderer.svelte';
  import ColorRenderer from './primitive/ColorRenderer.svelte';
  import WysiwygRenderer from './text/WysiwygRenderer.svelte';

  export let type: string; // e.g., "TEXT", "IMAGE", "RELATION"
  export let value: any;
  export let label: string;
  export let mode: string;
  export let relatedCollection = null;


  type RendererKey = 'TEXT' | 'TEXTAREA' | 'NUMBER' | 'DATE' | 'DATETIME' | 'TIME' | 
                    'IMAGE' | 'VIDEO' | 'AUDIO' | 'FILE' | 'DOCUMENT' | 
                    'RELATION' | 'MULTI_RELATION' | 'OBJECT' | 'ARRAY' | 
                    'JSON' | 'CHECKBOX' | 'URL' | 'EMAIL' | 'COLOR' | 'WYSIWYG';

  const registry: Record<string, any> = {
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
    OBJECT: ObjectRenderer,
    ARRAY: ArrayRenderer,
    JSON: JsonRenderer,
    CHECKBOX: BooleanRenderer,
    URL: UrlRenderer,
    EMAIL: EmailRenderer,
    COLOR: ColorRenderer,
    WYSIWYG: WysiwygRenderer
  };

  // 3. Cast the type to safely index the registry
  $: CurrentRenderer = registry[type as RendererKey] ?? TextRenderer;

</script>

<div class="renderer-wrapper">
  <svelte:component
    this={CurrentRenderer}
    {value}
    {relatedCollection}
    {label}
    {mode}
/>
</div>