<script lang="ts">
  import CollectionInput from "./CollectionInput.svelte";
  import EmbeddableInput from "./EmbeddableInput.svelte";
  import EnumInput from "./EnumInput.svelte";
  import ScalarInput from "./ScalarInput.svelte";
  import FileUploader from "./FileUploader.svelte";

  import { ValueType, type ValueDescriptor } from "../../types/resources";

  export let descriptor: ValueDescriptor;
  export let value: any;

  import { createEventDispatcher } from "svelte";
  import { isFileInputType } from "../../types/utils";
  const dispatch = createEventDispatcher();

  let previous = value;
  $: if (previous !== value) {
    previous = value;
    dispatch("change", value);
  }

  $: isFile = isFileInputType(descriptor.inputType) && !descriptor.collection;

  $: fileCol = {
    type: descriptor.inputType,
    fileOptions: descriptor.fileOptions ?? {}
  };

  function handleFileChange(e: CustomEvent<{ value: any }>) {
    value = e.detail.value;
  }
</script>

{#if isFile}
  <FileUploader col={fileCol} {value} on:change={handleFileChange} />

{:else if descriptor.collection}

  <CollectionInput descriptor={descriptor.collection} bind:value />

{:else if descriptor.type === ValueType.EMBEDDABLE}

  <EmbeddableInput descriptor={descriptor} bind:value />

{:else if descriptor.type === ValueType.ENUM}

  <EnumInput descriptor={descriptor} bind:value />

{:else}

  <ScalarInput descriptor={descriptor} bind:value />

{/if}

