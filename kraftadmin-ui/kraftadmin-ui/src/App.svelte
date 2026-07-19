<!-- <script lang="ts">
  import { onMount } from "svelte";
  import { location } from "svelte-spa-router";
  import Login from "./lib/components/Login.svelte";
  import AdminLayout from "./lib/components/AdminLayout.svelte";
  import { authMode, isBridgeMode } from "./lib/stores/authMode";
  import { isAuthenticated } from "./lib/stores/auth";
  import { adminSettings } from "./lib/stores/settings";
  import { updateResources } from "./lib/stores/resources";
  import { kraftFetch } from "./api";

  let bootstrapped = false;
  let descriptor: any = null;

  $: isLogin = $location === "/auth/login";

  onMount(async () => {
    try {
      const descRes = await fetch("/admin/api/resources/descriptors", {
        headers: { Accept: "application/json" },
        credentials: "same-origin"
      });

      if (descRes.ok) {
        const data = await descRes.json();
        descriptor = data;
        authMode.set(data.environment?.authMode ?? "unknown");
        isBridgeMode.set(data.environment?.authMode === "bridge");

        isAuthenticated.set(true);

        if (data.resources) updateResources(data.resources);
      } else {
        isAuthenticated.set(false);
      }

      const settingsRes = await kraftFetch("/admin/api/settings");
      if (settingsRes.ok) {
        adminSettings.set(await settingsRes.json());
      }
    } catch {
      isAuthenticated.set(false);
    } finally {
      bootstrapped = true;
    }
  });


 $: faviconHref = $adminSettings?.logoUrl || '/vite.svg';

</script>

<svelte:head>
  <link rel="icon" type="image/svg+xml" href={faviconHref} />
</svelte:head>

{#if !bootstrapped}
  <div class="flex h-screen items-center justify-center bg-bg-main">
    <div class="w-12 h-12 border-4 border-brand-primary/20 border-t-brand-primary rounded-full animate-spin"></div>
  </div>
{:else if isLogin}
  <Login />
{:else}
  <AdminLayout {descriptor} />
{/if} -->

<script lang="ts">
  import { onMount } from "svelte";
  import { location } from "svelte-spa-router";
  import Login from "./lib/components/Login.svelte";
  import AdminLayout from "./lib/components/AdminLayout.svelte";
  import { authMode, isBridgeMode } from "./lib/stores/authMode";
  import { isAuthenticated } from "./lib/stores/auth";
  import { adminSettings } from "./lib/stores/settings";
  import { updateResources } from "./lib/stores/resources";
  import { kraftFetch } from "./api";

  let bootstrapped = false;
  let descriptor: any = null;

  // Tracks whether we've EVER successfully loaded descriptors, so we know
  // to re-fetch after a login transition rather than trusting a stale
  // (possibly null, possibly pre-login) descriptor forever.
  let descriptorsLoaded = false;

  $: isLogin = $location === "/auth/login";

  async function loadDescriptorsAndSettings() {
    try {
      const descRes = await fetch("/admin/api/resources/descriptors", {
        headers: { Accept: "application/json" },
        credentials: "same-origin"
      });

      if (descRes.ok) {
        const data = await descRes.json();
        descriptor = data;
        descriptorsLoaded = true;
        authMode.set(data.environment?.authMode ?? "unknown");
        isBridgeMode.set(data.environment?.authMode === "bridge");
        isAuthenticated.set(true);

        if (data.resources) updateResources(data.resources);
      } else {
        descriptorsLoaded = false;
        isAuthenticated.set(false);
      }

      const settingsRes = await kraftFetch("/admin/api/settings");
      if (settingsRes.ok) {
        adminSettings.set(await settingsRes.json());
      }
    } catch {
      descriptorsLoaded = false;
      isAuthenticated.set(false);
    } finally {
      bootstrapped = true;
    }
  }

  onMount(() => {
    loadDescriptorsAndSettings();
  });

  // Re-fetch descriptors the moment the user is authenticated but we
  // haven't successfully loaded them yet (e.g. the initial onMount fetch
  // ran while unauthenticated and failed, and login just flipped
  // isAuthenticated to true afterward — that transition would otherwise
  // never trigger another fetch, since onMount only runs once for the
  // lifetime of this root component).
  $: if (bootstrapped && $isAuthenticated && !descriptorsLoaded) {
    loadDescriptorsAndSettings();
  }

  $: faviconHref = $adminSettings?.logoUrl || '/vite.svg';
</script>

<svelte:head>
  <link rel="icon" type="image/svg+xml" href={faviconHref} />
</svelte:head>

{#if !bootstrapped}
  <div class="flex h-screen items-center justify-center bg-bg-main">
    <div class="w-12 h-12 border-4 border-brand-primary/20 border-t-brand-primary rounded-full animate-spin"></div>
  </div>
{:else if isLogin}
  <Login />
{:else}
  <AdminLayout {descriptor} />
{/if}