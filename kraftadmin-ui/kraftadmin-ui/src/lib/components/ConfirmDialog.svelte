<script lang="ts">
    import { dialog, confirmDialog } from "../stores/dialog";
</script>

{#if $dialog}
<div class="fixed inset-0 z-50 flex items-center justify-center bg-black/40">

    <div class="w-full max-w-md rounded-xl bg-bg-surface border border-border-subtle">

        <div class="p-6">

            <h2 class="text-text-main">{$dialog.title}</h2>

            <p class="text-text-main">{$dialog.message}</p>

        </div>

        <div class="flex justify-end gap-3 p-4">

            <button
                class="btn-secondary"
                on:click={confirmDialog.cancel}>
                Cancel
            </button>

            <button
                class="btn-danger"
                on:click={confirmDialog.confirm}>
                Continue
            </button>

        </div>

    </div>

</div>
{/if}