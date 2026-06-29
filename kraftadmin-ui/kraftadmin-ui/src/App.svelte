<script lang="ts">
    import { location } from "svelte-spa-router";
    import Login from "./lib/components/Login.svelte";
    import AdminLayout from "./lib/components/AdminLayout.svelte";

    $: isLogin = $location === "/auth/login";
</script>

{#if isLogin}
    <Login />
{:else}
    <AdminLayout />
{/if}