<script lang="ts">
  import { onMount } from "svelte";
  import Router from "svelte-spa-router";
  import { routes } from "../../routes";
  import Sidebar from "../components/Sidebar.svelte";
  import Navbar from "../components/Navbar.svelte";
  import Footer from "../components/Footer.svelte";
  import FeedbackWidget from "../components/FeedbackWidget.svelte";
  import { kraftFetch } from "../../api";
  import { updateResources } from "../stores/resources";
  import { adminSettings } from "../stores/settings";
  import Snackbar from "./Snackbar.svelte";
  import ConfirmDialog from "./ConfirmDialog.svelte";
  import ActionInputDialog from "./actions/ActionInputDialog.svelte";

  export let descriptor: any = null;

  let isMobileMenuOpen = false;

  $: if ($adminSettings?.theme?.primaryColor) {
    document.documentElement.style.setProperty(
      "--brand-primary",
      $adminSettings.theme.primaryColor
    );
  }

  onMount(() => {
    const interval = setInterval(async () => {
      const res = await kraftFetch("/admin/api/resources/descriptors");
      if (res.ok) {
        const data = await res.json();
        updateResources(data.resources || []);
      }
    }, 15000);

    return () => clearInterval(interval);
  });
</script>

<svelte:head>
  <title>{descriptor?.title || "KraftAdmin"}</title>
</svelte:head>

<div class="flex h-screen bg-bg-main font-sans overflow-hidden">
  {#if isMobileMenuOpen}
    <button class="md:hidden fixed inset-0 bg-black/50 z-40" on:click={() => isMobileMenuOpen = false} />
  {/if}

  <aside class="{isMobileMenuOpen ? 'translate-x-0' : '-translate-x-full'} md:translate-x-0 fixed md:static inset-y-0 left-0 z-50 w-60 h-full transition-transform duration-300">
    <Sidebar on:close={() => isMobileMenuOpen = false} />
  </aside>

  <div class="flex-1 flex flex-col min-w-0">
    <Navbar on:toggle={() => isMobileMenuOpen = !isMobileMenuOpen} environment={descriptor?.environment} />
    <main class="flex-1 overflow-y-auto p-4 md:p-8">
      <Router {routes} />
    </main>
    <ConfirmDialog />
    <ActionInputDialog />
    <Snackbar />
    <FeedbackWidget />
    <Footer />
  </div>
</div>