<script lang="ts">
    import SmartInput from "./SmartInput.svelte";
    import { createEventDispatcher } from "svelte";
    import type { ValueDescriptor } from "../../types/descriptors";
    import { createDefaultValue } from "../../stores/createDefault";

    export let descriptor: ValueDescriptor;
    export let value: Record<string, any> | null = null;

    const dispatch = createEventDispatcher();

    // Unwraps a backend EmbeddedResponse ({ data, summary }) into its flat field map.
    function unwrapEmbedded(raw: any): Record<string, any> {
        if (raw != null && typeof raw === "object" && "data" in raw && "summary" in raw) {
            return raw.data ?? {};
        }
        return raw ?? {};
    }

    // Guarantee a non-null value, and unwrap any EMBEDDABLE sub-fields IN PLACE
    // on the real `value` object — never on a separate derived copy. Svelte's
    // bind:value on a child component only propagates changes made to the
    // exact prop identifier being bound; a copy silently breaks that contract
    // at every nesting level above it.
    $: if (value == null) {
        value = createDefaultValue(descriptor);
    }

    $: if (value) {
        for (const field of descriptor.fields) {
            if (field.value.type === "EMBEDDABLE") {
                const unwrapped = unwrapEmbedded(value[field.name]);
                if (unwrapped !== value[field.name]) {
                    value[field.name] = unwrapped;
                }
            }
        }
    }

    $: dispatch("change", value);
</script>

{#if value}
    <div class="p-4 rounded-xl border border-border-subtle bg-bg-main space-y-4">
        {#each descriptor.fields as field (field.name)}
            <div>
                <label class="field-label">
                    {field.label}
                </label>
                <SmartInput
                    descriptor={field.value}
                    bind:value={value[field.name]}
                />
            </div>
        {/each}
    </div>
{/if}