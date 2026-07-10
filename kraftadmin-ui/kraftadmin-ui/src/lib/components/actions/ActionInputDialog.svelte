<script lang="ts">
    import { actionDialog } from "../../stores/actionDialog";
      import FieldRenderer from "../inputs/FieldRenderer.svelte";


    let form: Record<string, any> = {};

    $: action = $actionDialog.action;
    $: open = $actionDialog.open;

    $: if (action?.input) {
        form = {};

        for (const field of action.input.fields) {
            form[field.name] =
                field.defaultValue ??
                (field.type === "CHECKBOX" ? false : null);
        }
    }

    function update(field: string, value: any) {
        form = {
            ...form,
            [field]: value
        };
    }

    function submit() {
        actionDialog.submit(form);
    }

    function cancel() {
        actionDialog.cancel();
    }
</script>

{#if open && action?.input}

<div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">

    <div class="w-full max-w-2xl rounded-xl border border-border-subtle bg-bg-surface shadow-2xl">

        <div class="border-b border-border-subtle px-6 py-5">

            <h2 class="text-xl text-text-muted">
                {action.input.title}
            </h2>

            {#if action.input.description}
                <p class="mt-2 text-sm text-text-muted">
                    {action.input.description}
                </p>
            {/if}

        </div>

        <div class="space-y-5 p-6 max-h-[70vh] overflow-y-auto">

            {#each action.input.fields as field}

                <div class="space-y-2">

                    <label class="block text-xs font-black uppercase tracking-widest text-text-muted">

                        {field.label}

                        {#if field.required}
                            <span class="text-danger">*</span>
                        {/if}

                    </label>

                    <FieldRenderer
                        col={field}
                        value={form[field.name]}
                        on:change={(e) =>
                            update(e.detail.field, e.detail.value)}
                    />

                    {#if field.helperText}
                        <p class="text-xs text-text-muted">
                            {field.helperText}
                        </p>
                    {/if}

                </div>

            {/each}

        </div>

        <div class="flex justify-end gap-3 border-t border-border-subtle px-6 py-5">

            <button
                class="btn-secondary"
                on:click={cancel}>

                {action.input.cancelLabel}

            </button>

            <button
                class="btn-primary"
                on:click={submit}>

                {action.input.submitLabel}

            </button>

        </div>

    </div>

</div>

{/if}