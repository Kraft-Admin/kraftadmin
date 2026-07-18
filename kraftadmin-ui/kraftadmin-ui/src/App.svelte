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

        //  A successful descriptor fetch proves
        // the existing session cookie is still valid — on a hard reload,
        // this is the ONLY code path that can ever set isAuthenticated to
        // true. Login.svelte only sets it after an explicit login POST,
        // which never runs again on a reload with an already-valid session.
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
{/if}