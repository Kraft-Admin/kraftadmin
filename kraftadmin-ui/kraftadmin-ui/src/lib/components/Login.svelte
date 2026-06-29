<script lang="ts">
  import { replace } from "svelte-spa-router";
  import { isAuthenticated } from "../stores/auth";
  import { kraftFetch } from "../../api";

  let username = "";
  let password = "";
  let error = "";
  let submittng = false;

  async function handleLogin() {
    submittng = true;
    error = "";

    try {
      const res = await kraftFetch("/admin/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ username, password })
      });

      if (res.ok) {
        isAuthenticated.set(true);

        // Navigate to the dashboard
        replace("/");
      } else {
        error = "Invalid credentials";
      }
    } catch (e) {
      console.error(e);
      error = "Server connection failed";
    } finally {
      submittng = false;
    }
  }
</script>

<div class="min-h-screen flex items-center justify-center bg-bg-main px-6 py-8">
  <div
    class="w-full max-w-md rounded-2xl border border-border-subtle bg-bg-surface shadow-2xl p-8">

    <!-- Logo -->
    <div class="text-center mb-8">
      <div
        class="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-2xl bg-brand-primary text-white shadow-lg">
        <span class="text-3xl font-black">K</span>
      </div>

      <h1 class="text-3xl font-bold tracking-tight text-text-main">
        Welcome to KraftAdmin
      </h1>

      <p class="mt-4 text-sm leading-6 text-text-main/70">
        Sign in to your administration workspace to manage resources,
        monitor system health, review analytics, configure settings,
        and oversee your application from a single unified dashboard.
      </p>
    </div>

    {#if error}
      <div
        class="mb-6 rounded-lg border border-danger/20 bg-danger/10 p-3 text-sm text-danger">
        {error}
      </div>
    {/if}

    <div class="space-y-5">

      <div>
        <label
          for="username"
          class="mb-2 block text-xs font-semibold uppercase tracking-wider text-text-main/70">
          Username or Email
        </label>

        <input
          id="username"
          bind:value={username}
          type="text"
          autocomplete="username"
          class="input-base w-full"
          placeholder="admin@example.com" />
      </div>

      <div>
        <label
          for="password"
          class="mb-2 block text-xs font-semibold uppercase tracking-wider text-text-main/70">
          Password
        </label>

        <input
          id="password"
          bind:value={password}
          type="password"
          autocomplete="current-password"
          class="input-base w-full"
          placeholder="Enter your password" />
      </div>

      <button
        on:click={handleLogin}
        disabled={submittng}
        class="mt-2 w-full rounded-lg bg-brand-primary py-3 font-semibold text-white transition-all hover:opacity-90 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50">

        {#if submittng}
          Authenticating...
        {:else}
          Sign In
        {/if}

      </button>

    </div>

    <div class="mt-8 border-t border-border-subtle pt-6">

      <div class="grid grid-cols-2 gap-4 text-sm">

        <div class="rounded-lg border border-border-subtle p-3">
          <div class="font-semibold text-brand-primary">
            Resource Management
          </div>

          <div class="mt-1 text-xs text-text-main/70">
            Create, update and manage application resources.
          </div>
        </div>

        <div class="rounded-lg border border-border-subtle p-3">
          <div class="font-semibold text-brand-primary">
            Analytics
          </div>

          <div class="mt-1 text-xs text-text-main/70">
            Monitor trends and application performance.
          </div>
        </div>

        <div class="rounded-lg border border-border-subtle p-3">
          <div class="font-semibold text-brand-primary">
            System Monitoring
          </div>

          <div class="mt-1 text-xs text-text-main/70">
            Review health, telemetry and operational metrics.
          </div>
        </div>

        <div class="rounded-lg border border-border-subtle p-3">
          <div class="font-semibold text-brand-primary">
            Secure Access
          </div>

          <div class="mt-1 text-xs text-text-main/70">
            Cookie-based authentication with protected sessions.
          </div>
        </div>

      </div>

      <p class="mt-6 text-center text-xs text-text-main/50">
        KraftAdmin is a modern administration framework for Spring Boot,
        providing resource management, auditing, analytics, telemetry,
        monitoring and application configuration through a unified interface.
      </p>

    </div>

  </div>
</div>