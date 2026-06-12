<script lang="ts">
  import { onMount, onDestroy } from "svelte";
  import Quill from 'quill';
  
  // standard styles
  import "quill/dist/quill.snow.css";

  // Form value and configuration bindings passed down by KraftAdmin fields
  export let value: string = "";
  export let config: any = null;
  export let onChange: (html: string) => void = () => {};

  let editorElement: HTMLDivElement;
  let quillInstance: Quill | null = null;

  onMount(() => {
    if (!editorElement) return;

    // A robust default toolbar layout in case the config payload arrives empty
    const defaultToolbar = [
      ['bold', 'italic', 'underline'],
      [{ 'list': 'ordered'}, { 'list': 'bullet' }],
      ['link', 'clean']
    ];

    // Safely pull the dynamic toolbar structure emitted by the ToolbarProfile enum
    const toolbarSelection = config?.options || defaultToolbar;

    // Initialize Quill with the dynamic settings array
    quillInstance = new Quill(editorElement, {
      modules: { toolbar: toolbarSelection },
      theme: 'snow',
      placeholder: config?.placeholder || 'Write rich text content here...'
    });

    // Set initial content safely
    quillInstance.root.innerHTML = value || '';

    // Emit changes upward to your dynamic forms
    quillInstance.on('text-change', () => {
      if (quillInstance) {
        onChange(quillInstance.root.innerHTML);
      }
    });
  });

  // Watch external value mutations (e.g., form resets or lazy async loading)
  $: if (quillInstance && value !== quillInstance.root.innerHTML) {
    quillInstance.root.innerHTML = value || '';
  }

  onDestroy(() => {
    if (quillInstance) {
      // Clear out the HTML elements to prevent memory leaks
      editorElement.innerHTML = '';
    }
  });
</script>

<div class="wysiwyg-editor-wrapper">
  <div bind:this={editorElement} class="wysiwyg-editor-surface"></div>
</div>

<style>
  .wysiwyg-editor-wrapper {
    border: 1px solid #27272a;
    border-radius: 0.5rem;
    background-color: #09090b;
    overflow: hidden;
  }
  .wysiwyg-editor-wrapper :global(.ql-toolbar.ql-snow) {
    background-color: #18181b;
    border: none;
    border-bottom: 1px solid #27272a;
    padding: 0.5rem;
  }
  .wysiwyg-editor-wrapper :global(.ql-container.ql-snow) {
    border: none;
    font-family: ui-sans-serif, system-ui, sans-serif;
    font-size: 0.875rem;
    color: #fafafa;
    min-height: 200px;
  }
  .wysiwyg-editor-wrapper :global(.ql-editor.ql-blank::before) {
    color: #71717a;
    font-style: normal;
  }

  /* Stylings to help distinguish active buttons inside dark mode */
  .wysiwyg-editor-wrapper :global(.ql-snow .ql-stroke) {
    stroke: #e4e4e7;
  }
  .wysiwyg-editor-wrapper :global(.ql-snow .ql-fill) {
    fill: #e4e4e7;
  }
  .wysiwyg-editor-wrapper :global(.ql-snow .ql-picker) {
    color: #e4e4e7;
  }

</style>