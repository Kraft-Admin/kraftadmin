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

        <div class="flex flex-wrap gap-1">
            {#each images.slice(0, 4) as image, i}
                <button
                    type="button"
                    on:click={() => open(i)}
                    class="focus:outline-none"
                >
                    <img
                        src={image}
                        alt=""
                        class="w-10 h-10 rounded-lg border border-border-subtle object-cover hover:scale-105 transition-transform"
                    />
                </button>
            {/each}

            {#if images.length > 4}
                <button
                    class="w-10 h-10 rounded-lg border border-border-subtle bg-bg-surface text-xs font-semibold"
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
                    class="group"
                >
                    <img
                        src={image}
                        alt=""
                        class="rounded-xl border border-border-subtle w-full aspect-square object-cover group-hover:opacity-90 transition"
                    />
                </button>
            {/each}
        </div>

    {/if}

    {#if fullscreen}

        <div
            class="fixed inset-0 z-[9999] bg-black/90 backdrop-blur-md flex items-center justify-center"
            on:click={close}
        >

            <!-- Close -->
            <button
                class="absolute top-6 right-6 w-10 h-10 rounded-full bg-white/10 hover:bg-white/20 text-white"
                on:click|stopPropagation={close}
            >
                ✕
            </button>

            <!-- Counter -->
            <div class="absolute top-6 left-1/2 -translate-x-1/2 text-white text-sm font-medium">
                {currentIndex + 1} / {images.length}
            </div>

            {#if images.length > 1}

                <!-- Previous -->
                <button
                    class="absolute left-6 w-12 h-12 rounded-full bg-white/10 hover:bg-white/20 text-white text-2xl"
                    on:click|stopPropagation={prev}
                >
                    ‹
                </button>

                <!-- Next -->
                <button
                    class="absolute right-6 w-12 h-12 rounded-full bg-white/10 hover:bg-white/20 text-white text-2xl"
                    on:click|stopPropagation={next}
                >
                    ›
                </button>

            {/if}

            <img
                src={images[currentIndex]}
                alt=""
                class="max-w-[92vw] max-h-[92vh] object-contain rounded-xl"
                on:click|stopPropagation
            />

        </div>

    {/if}

{:else}

    <span class="text-text-muted italic">—</span>

{/if}