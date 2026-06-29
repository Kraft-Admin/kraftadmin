<script lang="ts">
    import { onMount } from "svelte";
    import Router from "svelte-spa-router";
    import { routes } from "../../routes";

    import Sidebar from "../components/Sidebar.svelte";
    import Navbar from "../components/Navbar.svelte";
    import Footer from "../components/Footer.svelte";
    import FeedbackWidget from "../components/FeedbackWidget.svelte";

    import { kraftFetch } from "../../api";
    import { adminSettings } from "../stores/settings";
    import { isDark } from "../stores/theme";
    import { replace } from "svelte-spa-router";

    let descriptor: any = null;
    let loading = true;

    $: if ($adminSettings?.theme?.primaryColor) {
        document.documentElement.style.setProperty(
            "--brand-primary",
            $adminSettings.theme.primaryColor
        );
    }

    async function bootstrap() {
        loading = true;

        try {
            const [descRes, settingsRes] = await Promise.all([
                kraftFetch("/admin/api/resources/descriptors"),
                kraftFetch("/admin/api/settings")
            ]);

            if (!descRes.ok) {
                replace("/auth/login");
                return;
            }

            descriptor = await descRes.json();

            if (settingsRes.ok) {
                adminSettings.set(await settingsRes.json());
            }

        } finally {
            loading = false;
        }
    }

    onMount(bootstrap);
</script>

<svelte:head>
    <title>
        {loading
            ? "Loading KraftAdmin..."
            : descriptor?.title || "KraftAdmin"}
    </title>
</svelte:head>

{#if loading}
    <div class="flex h-screen flex-1 items-center justify-center bg-bg-main">
        <div class="flex flex-col items-center gap-4">
            <div class="w-12 h-12 border-4 border-brand-primary/20 border-t-brand-primary rounded-full animate-spin"></div>
            <div class="animate-pulse text-brand-primary font-black tracking-widest text-[10px] uppercase">
                Syncing KraftAdmin Environment...
            </div>
        </div>
    </div>
{:else}
<div class="flex h-screen bg-bg-main text-text-main font-sans overflow-hidden transition-colors duration-200 {$isDark ? 'dark' : ''}">

    <Sidebar
        resources={descriptor?.resources || []}
        title={descriptor?.title}
    />

    <div class="flex flex-1 flex-col min-w-0 relative">

        <Navbar environment={descriptor?.environment} />

        <main class="flex-1 overflow-y-auto p-8">
            <div class="px-4">
                <Router {routes}/>
            </div>
        </main>

        <FeedbackWidget/>
        <Footer/>

    </div>

</div>
{/if}