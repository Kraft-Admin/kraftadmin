<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { fade, slide } from 'svelte/transition';

  // State
  let logs: any[] = [];
  let activeFilter = 'ALL';
  let autoRefresh = true;
  let pollingInterval: any = null;
  let seenIds = new Set<string>();
  let expandedTrace: string | null = null;

  function processIncomingLogs(incoming: any[]) {
    // Correctly identifying uniqueness via trace_id or generated id
    const newItems = incoming.filter(item => {
      const log = item.event;
      // Use traceId as the primary unique key if available
      const id = log.traceId || log.id || `${log.timestamp}-${log.resource}`;
      if (seenIds.has(id)) return false;
      seenIds.add(id);
      return true;
    });

    if (newItems.length > 0) {
      // Merge and maintain the 500-item buffer
      logs = [...newItems, ...logs]
        .sort((a, b) => b.event.timestamp - a.event.timestamp)
        .slice(0, 500);
    }
  }

  async function fetchLogs() {
    if (!autoRefresh) return;

    try {
      // Endpoint reflects the backend FetchLatestWithQueries logic
      const res = await fetch('/admin/api/system/logs?limit=50');
      if (res.ok) {
        const data = await res.json();
        processIncomingLogs(data);
      }
    } catch (e) {
      console.error("Pulse sync failed", e);
    }
  }

  function startPolling() {
    fetchLogs();
    pollingInterval = setInterval(fetchLogs, 5000);
  }

  onMount(() => {
    startPolling();
  });

  onDestroy(() => {
    if (pollingInterval) clearInterval(pollingInterval);
    seenIds.clear();
  });

  // Reactive filtering
  $: filteredLogs = activeFilter === 'ALL'
    ? logs
    : logs.filter(item => (item.event.type) === activeFilter);

  const getLevelStyles = (type: string) => {
    switch (type) {
      case 'ERROR': return 'text-red-500 bg-red-500/10 border-red-500/20';
      case 'AUDIT': return 'text-emerald-500 bg-emerald-500/10 border-emerald-500/20';
      case 'SYSTEM': return 'text-blue-400 bg-blue-500/10 border-blue-500/20';
      case 'WARN':  return 'text-amber-500 bg-amber-500/10 border-amber-500/20';
      default:      return 'text-zinc-400 bg-zinc-500/10 border-zinc-500/20';
    }
  };

  const toggleTrace = (traceId: string) => {
    expandedTrace = expandedTrace === traceId ? null : traceId;
  };
</script>

<div class="flex flex-col h-full bg-bg-main border border-border-subtle rounded-3xl overflow-hidden shadow-2xl shadow-black/50">
  <!-- Header -->
  <div class="flex items-center justify-between p-4 border-b border-border-subtle bg-bg-surface/50 backdrop-blur-md">
    <div class="flex items-center gap-6">
      <div class="flex items-center gap-2">
        <div class="w-2 h-2 rounded-full bg-emerald-500 {autoRefresh ? 'animate-ping' : ''}"></div>
        <h2 class="text-xs font-black uppercase tracking-widest text-text-main">System Pulse</h2>
      </div>

      <nav class="flex p-1 bg-bg-main rounded-xl border border-border-subtle">
        {#each ['ALL', 'AUDIT', 'SYSTEM', 'ERROR'] as filter}
          <button
            on:click={() => activeFilter = filter}
            class="px-4 py-1.5 rounded-lg text-[10px] font-bold transition-all uppercase tracking-tight
            {activeFilter === filter ? 'bg-bg-surface text-brand-primary shadow-sm' : 'text-zinc-500 hover:text-zinc-300'}"
          >
            {filter}
          </button>
        {/each}
      </nav>
    </div>

    <button
      on:click={() => autoRefresh = !autoRefresh}
      class="text-[10px] font-black uppercase tracking-widest {autoRefresh ? 'text-brand-primary' : 'text-zinc-500 hover:text-white'}"
    >
      {autoRefresh ? 'Polling Active' : 'Paused'}
    </button>
  </div>

  <!-- Log Table -->
  <div class="flex-1 overflow-y-auto font-mono text-[11px] leading-relaxed custom-scrollbar">
    <table class="w-full border-separate border-spacing-0">
      <thead class="sticky top-0 bg-bg-surface text-zinc-500 border-b border-border-subtle z-10 text-left">
        <tr>
          <th class="px-6 py-3 font-black uppercase tracking-tighter w-24">Timestamp</th>
          <th class="px-6 py-3 font-black uppercase tracking-tighter w-20">Type</th>
          <th class="px-6 py-3 font-black uppercase tracking-tighter">Activity Trace</th>
        </tr>
      </thead>
      <tbody class="divide-y divide-zinc-900/50">
        {#each filteredLogs as item (item.event.traceId || item.event.id)}
          {@const log = item.event}
          {@const queries = item.queries}

          <tr
            transition:fade={{ duration: 150 }}
            class="hover:bg-white/5 transition-colors group cursor-pointer"
            on:click={() => toggleTrace(log.traceId)}
          >
            <td class="px-6 py-3 text-zinc-500 tabular-nums whitespace-nowrap">
              {new Date(log.timestamp).toLocaleTimeString('en-GB', { hour12: false })}
            </td>

            <td class="px-6 py-3">
              <span class="px-2 py-0.5 rounded border {getLevelStyles(log.type)} font-black text-[9px]">
                {log.type}
              </span>
            </td>

            <td class="px-6 py-3">
              <div class="flex items-center justify-between">
                <div class="flex flex-col">
                  <div class="flex items-center gap-2">
                    <span class="text-blue-400 font-bold">{log.action}</span>
                    <span class="text-zinc-300">{log.resource}</span>
                    <span class="text-[9px] text-zinc-600 px-1.5 py-0.5 bg-zinc-800 rounded">{log.durationMs}ms</span>
                    {#if log.ipAddress}
                      <span class="text-[9px] text-zinc-500 italic opacity-60 group-hover:opacity-100">{log.ipAddress}</span>
                    {/if}
                  </div>

                  {#if log.userAgent}
                    <div class="text-[8px] text-zinc-700 truncate max-w-[400px] uppercase mt-1">
                      {log.userAgent}
                    </div>
                  {/if}
                </div>

                <!-- Query Count Badge -->
                {#if queries && queries.length > 0}
                  <div class="flex items-center gap-1.5 px-2 py-1 bg-blue-500/10 border border-blue-500/20 rounded">
                    <span class="text-[9px] font-black text-blue-400">{queries.length} SQL</span>
                    <div class="w-1 h-1 rounded-full bg-blue-400 {queries.length > 5 ? 'animate-pulse' : ''}"></div>
                  </div>
                {/if}
              </div>
            </td>
          </tr>

          <!-- SQL Trace Dropdown -->
          {#if expandedTrace === log.traceId && queries?.length > 0}
            <tr>
              <td colspan="3" class="bg-black/40 px-6 py-6 border-l-2 border-brand-primary/50" transition:slide>
                <div class="flex flex-col gap-6">
                  <div class="flex justify-between items-center">
                    <div class="text-[9px] font-black text-zinc-500 uppercase tracking-widest">
                      Database Execution Trace ({log.traceId})
                    </div>
                    {#if log.referer}
                      <div class="text-[9px] text-zinc-600">Referer: {log.referer}</div>
                    {/if}
                  </div>

                  {#each queries as q}
                    <div class="space-y-2 group/query">
                      <div class="flex items-center gap-3 text-[10px]">
                        <span class="px-1.5 py-0.5 bg-emerald-500/10 text-emerald-500 rounded font-bold">{q.queryType}</span>
                        <span class="text-zinc-400">{q.durationMs}ms</span>
                        {#if q.isSlow}
                          <span class="text-red-500 animate-pulse font-black">[SLOW]</span>
                        {/if}
                        {#if q.tableName}
                          <span class="text-zinc-600 text-[9px]">Table: {q.tableName}</span>
                        {/if}
                      </div>
                      <div class="bg-zinc-950 p-4 rounded border border-zinc-800 text-zinc-300 break-all leading-normal whitespace-pre-wrap font-mono relative">
                        {q.sql}
                        <button class="absolute top-2 right-2 opacity-0 group-hover/query:opacity-100 text-[8px] text-zinc-500 uppercase">Copy</button>
                      </div>
                    </div>
                  {/each}
                </div>
              </td>
            </tr>
          {/if}
        {/each}
      </tbody>
    </table>

    {#if filteredLogs.length === 0}
      <div class="p-20 text-center text-zinc-600 uppercase tracking-widest font-black animate-pulse">
        Polling Telemetry Sink...
      </div>
    {/if}
  </div>
</div>

<style>
  .custom-scrollbar::-webkit-scrollbar { width: 4px; }
  .custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
  .custom-scrollbar::-webkit-scrollbar-thumb { background: #27272a; border-radius: 10px; }
  .custom-scrollbar::-webkit-scrollbar-thumb:hover { background: #3f3f46; }
</style>