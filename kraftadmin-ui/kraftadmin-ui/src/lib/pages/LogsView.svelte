<script lang="ts">
  import { onMount } from 'svelte';
  import { fade, slide } from 'svelte/transition';
  import { adminSettings } from '../stores/settings';

  type KraftEvent = {
    timestamp: string;
    type: string;
    resourceName?: string;
    traceId?: string;
    actorUsername?: string;
    actorRoles?: string[];
    tenantId?: string;
    ipAddress?: string;
    userAgent?: string;
    occurredAt?: string;

    [key: string]: unknown;
  };

  type KraftEventPage = {
    content: KraftEvent[];
    page: number;
    size: number;
    total: number;
  };

  let logs: KraftEvent[] = [];

  let activeFilter = 'ALL';

  let searchQuery = '';
  let resourceName = '';
  let actorUsername = '';

  let page = 0;
  const pageSize = 50;
  let total = 0;

  let loading = false;
  let error: string | null = null;

  let autoRefresh = true;
  let pollingInterval: ReturnType<typeof setInterval> | null = null;

  let expandedTrace: string | null = null;

  let abortController: AbortController | null = null;

  let lastEventTimestamp: string | null = null;

  $: isEnabled =
    $adminSettings?.telemetryConfig?.enabled ?? false;

  $: hasActiveSearch =
    searchQuery.trim().length > 0 ||
    resourceName.trim().length > 0 ||
    actorUsername.trim().length > 0 ||
    activeFilter !== 'ALL';

  function getEventCategory(type: string): string {
    if (type.includes('Failed')) {
      return 'ERROR';
    }

    if (
      type.includes('Create') ||
      type.includes('Update') ||
      type.includes('Delete') ||
      type.includes('Action')
    ) {
      return 'AUDIT';
    }

    return 'SYSTEM';
  }

  function getLevelStyles(type: string): string {
    switch (getEventCategory(type)) {
      case 'ERROR':
        return 'text-red-500 bg-red-500/10 border-red-500/20';

      case 'AUDIT':
        return 'text-emerald-500 bg-emerald-500/10 border-emerald-500/20';

      case 'SYSTEM':
        return 'text-blue-400 bg-blue-500/10 border-blue-500/20';

      default:
        return 'text-zinc-400 bg-zinc-500/10 border-zinc-500/20';
    }
  }

  function eventId(log: KraftEvent): string {
    return [
      log.timestamp,
      log.type,
      log.resourceName ?? '',
      log.traceId ?? '',
      log.actorUsername ?? ''
    ].join('|');
  }

  function formatTime(timestamp: string): string {
    return new Date(timestamp).toLocaleTimeString(
      'en-GB',
      {
        hour12: false
      }
    );
  }

  function formatDate(timestamp: string): string {
    return new Date(timestamp).toLocaleDateString(
      'en-GB',
      {
        day: '2-digit',
        month: 'short',
        year: 'numeric'
      }
    );
  }

  function formatEventType(type: string): string {
    return type
      .replace(/([a-z])([A-Z])/g, '$1 $2')
      .toUpperCase();
  }

  function formatValue(value: unknown): string {
    if (value === null || value === undefined) {
      return 'null';
    }

    if (typeof value === 'object') {
      return JSON.stringify(value, null, 2);
    }

    return String(value);
  }

  function toggleTrace(traceId: string | undefined) {
    if (!traceId) {
      return;
    }

    expandedTrace =
      expandedTrace === traceId
        ? null
        : traceId;
  }

  function updateLastTimestamp(events: KraftEvent[]) {
    if (events.length === 0) {
      return;
    }

    const latest = events.reduce((latest, current) => {
      return new Date(current.timestamp) > new Date(latest.timestamp)
        ? current
        : latest;
    });

    lastEventTimestamp = latest.timestamp;
  }

  /**
   * Fetch the first page or a searched page.
   *
   * This is NOT used for live polling.
   */
  async function searchEvents() {
    if (!isEnabled || loading) {
      return;
    }

    abortController?.abort();

    abortController = new AbortController();

    loading = true;
    error = null;

    try {
      const params = new URLSearchParams();

      params.set('page', String(page));
      params.set('size', String(pageSize));

      if (searchQuery.trim()) {
        params.set(
          'query',
          searchQuery.trim()
        );
      }

      if (activeFilter !== 'ALL') {
        params.set(
          'category',
          activeFilter
        );
      }

      if (resourceName.trim()) {
        params.set(
          'resourceName',
          resourceName.trim()
        );
      }

      if (actorUsername.trim()) {
        params.set(
          'actorUsername',
          actorUsername.trim()
        );
      }

      const response = await fetch(
        `/admin/api/events?${params.toString()}`,
        {
          signal: abortController.signal
        }
      );

      if (!response.ok) {
        throw new Error(
          `Failed to fetch events: ${response.status}`
        );
      }

      const data: KraftEventPage =
        await response.json();

      logs = data.content;
      total = data.total;

      updateLastTimestamp(data.content);

    } catch (e) {
      if (
        e instanceof DOMException &&
        e.name === 'AbortError'
      ) {
        return;
      }

      error =
        e instanceof Error
          ? e.message
          : 'Failed to load events';

      console.error(
        'KraftAdmin event search failed',
        e
      );

    } finally {
      loading = false;
    }
  }

  /**
   * Fetch only new events.
   *
   * This is the important part of the live UI.
   */
  async function fetchLatestEvents() {
    if (
      !isEnabled ||
      !autoRefresh ||
      hasActiveSearch
    ) {
      return;
    }

    try {
      const params = new URLSearchParams();

      if (lastEventTimestamp) {
        params.set(
          'after',
          lastEventTimestamp
        );
      }

      params.set(
        'limit',
        '100'
      );

      const response = await fetch(
        `/admin/api/events/latest?${params.toString()}`
      );

      if (!response.ok) {
        return;
      }

      const incoming: KraftEvent[] =
        await response.json();

      if (incoming.length === 0) {
        return;
      }

      const existingIds =
        new Set(logs.map(eventId));

      const newEvents =
        incoming.filter(event => {
          return !existingIds.has(
            eventId(event)
          );
        });

      if (newEvents.length === 0) {
        return;
      }

      logs = [
        ...newEvents,
        ...logs
      ]
        .sort(
          (a, b) =>
            new Date(b.timestamp).getTime() -
            new Date(a.timestamp).getTime()
        )
        .slice(0, 500);

      total += newEvents.length;

      updateLastTimestamp(newEvents);

    } catch (e) {
      console.error(
        'KraftAdmin live event polling failed',
        e
      );
    }
  }

  function startPolling() {
    stopPolling();

    if (
      isEnabled &&
      autoRefresh &&
      !hasActiveSearch
    ) {
      pollingInterval =
        setInterval(
          fetchLatestEvents,
          5000
        );
    }
  }

  function stopPolling() {
    if (pollingInterval) {
      clearInterval(
        pollingInterval
      );

      pollingInterval = null;
    }
  }

  function toggleAutoRefresh() {
    autoRefresh = !autoRefresh;

    if (autoRefresh) {
      startPolling();
    } else {
      stopPolling();
    }
  }

  function applyFilters() {
    page = 0;

    stopPolling();

    searchEvents();
  }

  function clearFilters() {
    searchQuery = '';
    resourceName = '';
    actorUsername = '';
    activeFilter = 'ALL';
    page = 0;

    searchEvents();

    if (autoRefresh) {
      startPolling();
    }
  }

  function nextPage() {
    if (
      (page + 1) * pageSize >= total ||
      loading
    ) {
      return;
    }

    page += 1;

    searchEvents();
  }

  function previousPage() {
    if (
      page === 0 ||
      loading
    ) {
      return;
    }

    page -= 1;

    searchEvents();
  }

  onMount(() => {
    if (isEnabled) {
      searchEvents();
    }

    return () => {
      stopPolling();

      abortController?.abort();
    };
  });

  $: if (
    isEnabled &&
    autoRefresh &&
    !hasActiveSearch &&
    !pollingInterval
  ) {
    startPolling();
  }

  $: if (
    !isEnabled ||
    !autoRefresh ||
    hasActiveSearch
  ) {
    stopPolling();
  }
</script>


<div class="flex flex-col h-full bg-bg-main border border-border-subtle rounded-3xl overflow-hidden shadow-[0_25px_50px_-12px_var(--shadow)]">

  {#if !isEnabled}
    <div class="flex-1 flex flex-col items-center justify-center p-8 text-center">
      <div class="text-4xl mb-4">🔇</div>
      <h2 class="text-xs font-black uppercase tracking-widest text-text-muted">Pulse Disabled</h2>
      <p class="text-[10px] text-text-muted mt-2">Enable system telemetry to view real-time activity logs.</p>
    </div>
  {:else}
    <!-- HEADER -->
    <div class="flex flex-col border-b border-border-subtle bg-bg-surface/50 backdrop-blur-md">
      <div class="flex items-center justify-between p-4">
        <div class="flex items-center gap-6">
          <div class="flex items-center gap-2">
            <div class="w-2 h-2 rounded-full bg-success {autoRefresh && !hasActiveSearch ? 'animate-ping' : ''}"></div>
            <h2 class="text-xs font-black uppercase tracking-widest text-text-main">System Pulse</h2>
          </div>
          <nav class="flex p-1 bg-bg-main rounded-xl border border-border-subtle">
            {#each ['ALL', 'AUDIT', 'SYSTEM', 'ERROR'] as filter}
              <button
                on:click={() => { activeFilter = filter; page = 0; applyFilters(); }}
                class="px-4 py-1.5 rounded-lg text-[10px] font-bold transition-all uppercase tracking-tight
                {activeFilter === filter ? 'bg-bg-surface text-brand-primary shadow-sm' : 'text-text-muted hover:text-text-main'}"
              >
                {filter}
              </button>
            {/each}
          </nav>
        </div>
        <button on:click={toggleAutoRefresh} class="text-[10px] font-black uppercase tracking-widest {autoRefresh ? 'text-brand-primary' : 'text-text-muted hover:text-text-main'}">
          {autoRefresh ? (hasActiveSearch ? 'Polling Paused' : 'Polling Active') : 'Paused'}
        </button>
      </div>

      <!-- SEARCH -->
      <div class="flex gap-2 px-4 pb-4">
        <input bind:value={searchQuery} placeholder="Search events..." class="input-base flex-1 text-xs" on:keydown={(e) => e.key === 'Enter' && applyFilters()} />
        <input bind:value={resourceName} placeholder="Resource" class="input-base w-40 text-xs" on:keydown={(e) => e.key === 'Enter' && applyFilters()} />
        <input bind:value={actorUsername} placeholder="Actor" class="input-base w-48 text-xs" on:keydown={(e) => e.key === 'Enter' && applyFilters()} />
        <button on:click={applyFilters} class="btn-primary text-[10px] font-black uppercase">Search</button>
        {#if hasActiveSearch}
          <button on:click={clearFilters} class="px-4 py-2 rounded-lg border border-border-subtle text-text-muted text-[10px] font-black uppercase tracking-widest hover:text-text-main">Clear</button>
        {/if}
      </div>
    </div>

    <!-- EVENTS -->
    <div class="flex-1 overflow-y-auto font-mono text-[11px] leading-relaxed custom-scrollbar">
      {#if error}
        <div class="p-6 text-center text-danger text-xs">{error}</div>
      {:else}
        <table class="w-full border-separate border-spacing-0">
          <thead class="sticky top-0 bg-bg-surface text-text-muted border-b border-border-subtle z-10 text-left">
            <tr>
              <th class="px-6 py-3 font-black uppercase tracking-tighter w-36">Timestamp</th>
              <th class="px-6 py-3 font-black uppercase tracking-tighter w-32">Type</th>
              <th class="px-6 py-3 font-black uppercase tracking-tighter">Activity Trace</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-border-subtle">
            {#each logs as log (eventId(log))}
              <tr transition:fade={{ duration: 150 }} class="hover:bg-bg-main transition-colors group cursor-pointer" on:click={() => toggleTrace(log.traceId)}>
                <td class="px-6 py-3 text-text-muted tabular-nums whitespace-nowrap">
                  <div>{formatTime(log.timestamp)}</div>
                  <div class="text-[9px] text-text-muted/60">{formatDate(log.timestamp)}</div>
                </td>
                <td class="px-6 py-3">
                  <span class="px-2 py-0.5 rounded border {getLevelStyles(log.type).replace('red-500','danger').replace('emerald-500','success').replace('blue-400','info').replace('zinc-400','text-muted')} font-black text-[9px]">{formatEventType(log.type)}</span>
                </td>
                <td class="px-6 py-3">
                  <div class="flex items-center gap-2">
                    <span class="text-info font-bold">{log.type}</span>
                    <span class="text-text-main">{log.resourceName ?? 'Unknown'}</span>
                    <span class="text-[9px] text-text-muted">{log.actorUsername ?? ''}</span>
                  </div>
                </td>
              </tr>
              {#if expandedTrace === log.traceId}
                <tr>
                  <td colspan="3" class="bg-bg-main px-6 py-6 border-l-2 border-brand-primary" transition:slide>
                    <pre class="bg-bg-surface p-4 rounded border border-border-subtle text-text-main text-[11px] overflow-x-auto">{formatValue(log)}</pre>
                  </td>
                </tr>
              {/if}
            {/each}
          </tbody>
        </table>
      {/if}
    </div>

    <!-- PAGINATION -->
    <div class="flex items-center justify-between border-t border-border-subtle bg-bg-surface/50 px-4 py-3">
      <span class="text-[10px] text-text-muted">{total} total events</span>
      <div class="flex items-center gap-2">
        <button on:click={previousPage} disabled={page === 0 || loading} class="btn-secondary text-[10px] py-1">Previous</button>
        <button on:click={nextPage} disabled={(page + 1) * pageSize >= total || loading} class="btn-secondary text-[10px] py-1">Next</button>
      </div>
    </div>
  {/if}
</div>