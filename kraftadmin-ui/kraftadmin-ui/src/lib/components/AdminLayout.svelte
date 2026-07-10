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
    import { updateResources } from "../stores/resources"; // Import the store helper
    import { replace } from "svelte-spa-router";

    import { Menu, X } from "lucide-svelte";
  import Snackbar from "./Snackbar.svelte";
  import ConfirmDialog from "./ConfirmDialog.svelte";
  import ActionInputDialog from "./actions/ActionInputDialog.svelte";
    
    let isMobileMenuOpen = false;

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

            const data = await descRes.json();
            descriptor = data;

            // Populate the store so the Sidebar updates reactively
            if (data.resources) {
                updateResources(data.resources);
            }

            if (settingsRes.ok) {
                adminSettings.set(await settingsRes.json());
            }

        } finally {
            loading = false;
        }
    }

    // Optional: Add polling here to update resource counts automatically
    onMount(() => {
        bootstrap();
        const interval = setInterval(async () => {
            const res = await kraftFetch("/admin/api/resources/descriptors");
            if (res.ok) {
                const data = await res.json();
                updateResources(data.resources || []);
            }
        }, 15000); // Poll every 15 seconds
        
        return () => clearInterval(interval);
    });
</script>

<svelte:head>
    <title>{loading ? "Loading KraftAdmin..." : descriptor?.title || "KraftAdmin"}</title>
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
<div class="flex h-screen bg-bg-main font-sans overflow-hidden">
    {#if isMobileMenuOpen}
        <button 
            class="md:hidden fixed inset-0 bg-black/50 z-40" 
            on:click={() => isMobileMenuOpen = false} 
        />
    {/if}

    <aside class="{isMobileMenuOpen ? 'translate-x-0' : '-translate-x-full'} 
        md:translate-x-0 fixed md:static inset-y-0 left-0 z-50 w-60 h-full transition-transform duration-300">
        <Sidebar on:close={() => isMobileMenuOpen = false} />
    </aside>

    <div class="flex-1 flex flex-col min-w-0">
        <Navbar 
            on:toggle={() => isMobileMenuOpen = !isMobileMenuOpen} 
            environment={descriptor?.environment}
        />
        <main class="flex-1 overflow-y-auto p-4 md:p-8">
            <Router {routes}/>
        </main>
        <ConfirmDialog/>
        <ActionInputDialog />
        <Snackbar />
         <FeedbackWidget/>
        <Footer/>
    </div>
</div>

{/if}