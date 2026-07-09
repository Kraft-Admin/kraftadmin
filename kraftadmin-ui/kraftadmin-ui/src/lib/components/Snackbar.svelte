<script lang="ts">
    import { snackbar } from "../stores/snackbar";
</script>

<div class="fixed top-4 right-4 z-[9999] flex flex-col gap-3">
    {#each $snackbar as item (item.id)}
        <div
            class="
                rounded-xl
                shadow-xl
                px-5
                py-3
                text-sm
                font-semibold
                text-white
                animate-fade-in
                {item.type === 'success' ? 'bg-green-600' : ''}
                {item.type === 'error' ? 'bg-red-600' : ''}
                {item.type === 'warning' ? 'bg-yellow-600 text-black' : ''}
                {item.type === 'info' ? 'bg-blue-600' : ''}
            "
        >
            {item.message}
        </div>
    {/each}
</div>