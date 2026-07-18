<script lang="ts">
    import { onMount } from "svelte";

    export let value: string | string[] | null = null;
    export let mode: "table" | "detail" = "detail";

    let fullscreen = false;
    let currentIndex = 0;

    $: images = Array.isArray(value)
        ? value.filter(Boolean)
        : value
            ? [value]
            : [];

    function open(index = 0) {
        currentIndex = index;
        fullscreen = true;
    }

    function close() {
        fullscreen = false;
    }

    function next() {
        if (images.length === 0) return;
        currentIndex = (currentIndex + 1) % images.length;
    }

    function prev() {
        if (images.length === 0) return;
        currentIndex = (currentIndex - 1 + images.length) % images.length;
    }

    onMount(() => {
        const handler = (e: KeyboardEvent) => {
            if (!fullscreen) return;
            if (e.key === "Escape") close();
            if (e.key === "ArrowRight") next();
            if (e.key === "ArrowLeft") prev();
        };

        window.addEventListener("keydown", handler);
        return () => window.removeEventListener("keydown", handler);
    });
</script>

{#if images.length > 0}
    {#if mode === "table"}
        <div class="flex flex-wrap gap-2">
            {#each images.slice(0, 4) as image, i}
                <button
                    type="button"
                    on:click={() => open(i)}
                    class="focus:outline-none transition-transform hover:scale-105"
                >
                    <img
                        src={image}
                        alt=""
                        class="w-10 h-10 rounded-lg border border-border-subtle object-cover"
                    />
                </button>
            {/each}

            {#if images.length > 4}
                <button
                    type="button"
                    class="w-10 h-10 rounded-lg border border-border-subtle bg-bg-surface text-text-muted text-xs font-semibold hover:bg-bg-main transition"
                    on:click={() => open(4)}
                >
                    +{images.length - 4}
                </button>
            {/if}
        </div>
    {:else}
        <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
            {#each images as image, i}
                <button
                    type="button"
                    on:click={() => open(i)}
                    class="group relative overflow-hidden rounded-xl border border-border-subtle"
                >
                    <img
                        src={image}
                        alt=""
                        class="w-full aspect-square object-cover transition duration-300 group-hover:scale-105"
                    />
                </button>
            {/each}
        </div>
    {/if}

    {#if fullscreen}
        <!-- Backdrop -->
        <div
            class="fixed inset-0 z-[9999] bg-overlay backdrop-blur-md flex items-center justify-center p-4"
            on:click={close}
            role="presentation"
        >
            <!-- Close Button -->
            <button
                class="absolute top-6 right-6 w-10 h-10 rounded-full bg-[var(--brand-primary)] hover:opacity-90 text-[var(--bg-surface)] flex items-center justify-center transition-all shadow-lg"
                on:click|stopPropagation={close}
                aria-label="Close"
            >
                ✕
            </button>

            <!-- Counter -->
            <div class="absolute top-6 left-1/2 -translate-x-1/2 text-[var(--text-main)] bg-[var(--bg-surface)] px-3 py-1 rounded-full shadow-sm text-sm font-medium">
                {currentIndex + 1} / {images.length}
            </div>

            {#if images.length > 1}
                <button
                    class="absolute left-6 w-12 h-12 rounded-full bg-[var(--brand-primary)] hover:opacity-90 text-[var(--bg-surface)] text-2xl flex items-center justify-center transition-all shadow-lg"
                    on:click|stopPropagation={prev}
                    aria-label="Previous"
                >
                    ‹
                </button>
                <button
                    class="absolute right-6 w-12 h-12 rounded-full bg-[var(--brand-primary)] hover:opacity-90 text-[var(--bg-surface)] text-2xl flex items-center justify-center transition-all shadow-lg"
                    on:click|stopPropagation={next}
                    aria-label="Next"
                >
                    ›
                </button>
            {/if}

            <img
                src={images[currentIndex]}
                alt="Full screen preview"
                class="max-w-[92vw] max-h-[92vh] object-contain rounded-xl"
                on:click|stopPropagation
            />
        </div>
    {/if}
{:else}
    <span class="text-text-muted italic">—</span>
{/if}