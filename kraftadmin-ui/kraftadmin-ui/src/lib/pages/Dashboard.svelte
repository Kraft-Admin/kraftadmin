<script lang="ts">
  import { onMount } from 'svelte';
  import { adminSettings } from '../stores/settings';
  import { Loader } from 'lucide-svelte';
  
  let dashboardData: any = null;
  let loading = true;

  onMount(async () => {
    try {
      const res = await fetch('/admin/api/dashboard');
      if (res.ok) dashboardData = await res.json();
    } finally {
      loading = false;
    }
  });
</script>

<div class="p-4 md:p-8 space-y-8 min-h-screen bg-bg-main transition-colors duration-300">
  {#if loading}
    <div class="flex flex-col items-center justify-center py-20 gap-4">
        <Loader class="w-8 h-8 text-brand-primary animate-spin" />
        <p class="text-[10px] text-text-muted font-black uppercase tracking-[0.2em]">Loading...</p>
      </div>
  {:else if dashboardData}
    <div class="flex flex-col md:flex-row md:justify-between md:items-end gap-6">
      <div>
        <h1 class="text-3xl font-black text-text-main uppercase tracking-tighter">
          {dashboardData.title}
        </h1>
        <p class="text-text-muted text-sm">{dashboardData.welcomeMessage}</p>
      </div>

      <!-- <div class="flex flex-wrap gap-3">
        <a href="#/system" class="btn-secondary text-xs">System Vitals</a>
        <a href="#/analytics" class="btn-secondary text-xs">Analytics</a>
        <a href="#/telemetry" class="btn-primary text-xs">Cloud Pulse</a>
      </div> -->
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-6">
      {#each dashboardData.stats as stat}
        <div class="bg-bg-surface border border-border-subtle p-6 rounded-3xl shadow-sm">
          <span class="text-text-muted text-[10px] font-black uppercase tracking-widest">{stat.label}</span>
          <div class="flex items-center justify-between mt-2">
            <span class="text-4xl font-black text-text-main">{stat.value}</span>
          </div>
        </div>
      {/each}
    </div>

    <div class="bg-bg-surface border border-border-subtle rounded-3xl overflow-hidden shadow-sm">
      <div class="p-6 border-b border-border-subtle flex justify-between items-center bg-bg-main/50">
        <h2 class="text-xl font-bold text-text-main">Library Capabilities</h2>
        <span class="text-[10px] font-bold text-text-muted uppercase italic">v{$adminSettings?.version || '0.1.0'}</span>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full text-left min-w-[500px]">
          <thead class="bg-bg-main/50 text-text-muted text-[10px] uppercase font-black">
            <tr>
              <th class="px-6 py-4">Feature</th>
              <th class="px-6 py-4">Status</th>
              <th class="px-6 py-4 text-right">Requirement</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-border-subtle">
            {#each dashboardData.features as feature}
              <tr class="hover:bg-bg-main transition-colors">
                <td class="px-6 py-4 text-text-main font-bold">{feature.name}</td>
                <td class="px-6 py-4">
                  <span class="chip !bg-opacity-10 !text-success">
                    {feature.status}
                  </span>
                </td>
                <td class="px-6 py-4 text-xs text-text-muted italic text-right">{feature.unlockCriteria || 'None'}</td>
              </tr>
            {/each}
          </tbody>
        </table>
      </div>
    </div>
  {/if}
</div>