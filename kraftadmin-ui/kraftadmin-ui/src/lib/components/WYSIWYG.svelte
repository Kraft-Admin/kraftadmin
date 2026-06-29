<script lang="ts">
  import { onMount, onDestroy } from "svelte";
  import Quill from "quill";

  import "quill/dist/quill.snow.css";

  export let value = "";
  export let config: any = {};
  export let onChange: (html: string) => void = () => {};

  let editorElement: HTMLDivElement;
  let quill: Quill;

  onMount(() => {
    const defaultToolbar = [
      ["bold", "italic", "underline", "strike"],
      [{ header: [1, 2, 3, false] }],
      [{ list: "ordered" }, { list: "bullet" }],
      [{ indent: "-1" }, { indent: "+1" }],
      [{ color: [] }, { background: [] }],
      [{ align: [] }],
      ["blockquote", "code-block"],
      ["link", "image"],
      ["clean"]
    ];

    quill = new Quill(editorElement, {
      theme: "snow",
      placeholder:
        config?.placeholder ?? "Write rich text content here...",
      modules: {
        toolbar: config?.options ?? defaultToolbar
      }
    });

    if (value) {
      quill.clipboard.dangerouslyPasteHTML(value);
    }

    quill.on("text-change", () => {
      onChange(quill.root.innerHTML);
    });
  });

  $: if (quill && value !== quill.root.innerHTML) {
    const selection = quill.getSelection();

    quill.clipboard.dangerouslyPasteHTML(value || "");

    if (selection) {
      quill.setSelection(selection);
    }
  }

  onDestroy(() => {
    quill?.off("text-change");
  });
</script>

<div class="wysiwyg-editor-wrapper">
  <div bind:this={editorElement}></div>
</div>