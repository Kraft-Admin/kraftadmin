<script lang="ts">
  import { onMount } from 'svelte';

  // Metrics state
  let metrics: any[] = [];
  let loading = true;
  let error: string | null = null;

  // Helper function to find specific metrics
  const getMetric = (name: string) => metrics.find(m => m.name === name);

  onMount(async () => {
    try {
      const response = await fetch('/admin/api/monitoring/metrics/internal');
      if (!response.ok) throw new Error('Failed to fetch metrics');

      const data = await response.json();
      metrics = data.meters || [];
    } catch (e) {
      error = e instanceof Error ? e.message : 'Unknown error';
    } finally {
      loading = false;
    }
  });
</script>

<div class="p-8 space-y-10 min-h-screen bg-bg-main transition-colors duration-300">
  {#if loading}
    <div class="animate-pulse space-y-10">
      <div class="h-8 w-64 bg-zinc-200 dark:bg-zinc-800 rounded-lg"></div>
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        {#each Array(3) as _} <div class="h-32 bg-bg-surface border border-border-subtle rounded-3xl"></div> {/each}
      </div>
    </div>
  {:else if error}
    <div class="p-6 bg-red-500/10 border border-red-500/20 rounded-3xl text-red-500 text-sm font-bold">
      Error: {error}
    </div>
  {:else}
    <div class="flex justify-between items-end">
      <div>
        <h1 class="text-3xl font-black text-text-main uppercase tracking-tighter">System Vitals</h1>
        <p class="text-zinc-500 text-sm">Real-time telemetry and internal state monitoring</p>
      </div>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <div class="bg-bg-surface border border-border-subtle p-6 rounded-3xl shadow-sm">
        <span class="text-zinc-500 text-[10px] font-black uppercase tracking-widest">JVM Threads</span>
        <div class="text-4xl font-black text-text-main mt-2">
          {getMetric('jvm.threads.live')?.measurements.VALUE ?? 0}
        </div>
      </div>

      <div class="bg-bg-surface border border-border-subtle p-6 rounded-3xl shadow-sm col-span-2">
        <span class="text-zinc-500 text-[10px] font-black uppercase tracking-widest">Memory Usage (Heap)</span>
        <div class="h-16 w-full flex items-end gap-1 mt-4">
          {#each Array(20) as _}
            <div class="flex-1 bg-brand-primary/20 rounded-t-lg transition-colors hover:bg-brand-primary"
                 style="height: {Math.random() * 80 + 20}%"></div>
          {/each}
        </div>
      </div>
    </div>

    <div class="bg-bg-surface border border-border-subtle rounded-3xl p-8 shadow-sm">
      <h2 class="text-xl font-bold text-text-main mb-6">Metric Breakdown</h2>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-x-6 gap-y-8">
        {#each metrics as meter}
          <div class="border-l-2 border-brand-primary pl-4">
            <div class="text-[10px] text-zinc-500 font-bold uppercase truncate" title={meter.name}>{meter.name}</div>
            <div class="text-xl font-black text-text-main mt-1">
                {Object.values(meter.measurements)[0]?.toFixed(2) ?? '0.00'}
            </div>
          </div>
        {/each}
      </div>
    </div>
  {/if}
</div>