<script lang="ts">
  import { link } from 'svelte-spa-router';
  import Cell from '../../lib/components/Cell.svelte';
  import Render from '../renderers/Render.svelte';
  import { Loader } from '@lucide/svelte';

  export let params: { name?: string } = {};

  let resource: any = null;
  let items: any[] = [];
  let columns: any[] = [];
  let currentPage = 1;
  let searchQuery = "";
  let sortField: string | null = null;
  let sortDirection: 'ASC' | 'DESC' | null = null;
  let debounceTimer: number;
  let pagination = { total: 0, pageSize: 20, totalPages: 0 };
  let loading = true;

  // 1. Trigger ONLY when the resource name changes
  $: if (params.name) {
      currentPage = 1; 
      searchQuery = "";
      sortField = null;
      sortDirection = null;
      loadData(params.name, 1, "");
  }

  $: placeholder = resource?.searchableFields?.length > 0 
    ? `Search by ${resource.searchableFields.join(', ')}...` 
    : "Search records...";

  // 2. Handle pagination changes
  async function handlePageChange(newPage: number) {
    if (newPage >= 1 && newPage <= pagination.totalPages && newPage !== currentPage) {
      currentPage = newPage;
      await loadData(params.name!, newPage, searchQuery, sortField ?? undefined, sortDirection ?? undefined);
    }
  }

  // 3. Handle search input with debouncing
  function handleSearch(event: any) {
    clearTimeout(debounceTimer);
    searchQuery = event.target.value;
    debounceTimer = setTimeout(() => {
      currentPage = 1;
      loadData(params.name!, currentPage, searchQuery, sortField ?? undefined, sortDirection ?? undefined);
    }, 500);
  }

  // 4. Handle column sorting
  function handleSort(columnName: string) {
    if (!resource?.sortableFields?.includes(columnName)) return;

    if (sortField === columnName) {
      sortDirection = sortDirection === 'ASC' ? 'DESC' : 'ASC';
    } else {
      sortField = columnName;
      sortDirection = 'DESC';
    }
    loadData(params.name!, currentPage, searchQuery, sortField, sortDirection ?? undefined);
  }

  async function loadData(resourceName: string, page: number, query: string, sField?: string, sDir?: string) {
    loading = true;
    try {
      let url = `/admin/api/resources/${resourceName}?page=${page}&size=${pagination.pageSize}`;
      if (query) url += `&q=${encodeURIComponent(query)}`;
      if (sField) url += `&sortField=${sField}&sortDirection=${sDir || 'DESC'}`;
      
      const response = await fetch(url);
      const result = await response.json();

      resource = result.resource;
      columns = resource.columns.filter((c: any) => c.visible);
      items = resource.data.items;
      
      pagination = {
        total: resource.data.total,
        pageSize: resource.data.pageSize,
        totalPages: resource.data.totalPages
      };
    } catch (e) {
      console.error("Error loading resource data:", e);
    } finally {
      loading = false;
    }
  }

  async function handleDelete(id: any) {
    if (!confirm(`Are you sure you want to delete?`)) return;
    const res = await fetch(`/admin/api/resources/${params.name}/${id}`, { method: 'DELETE' });
    if (res.ok) loadData(params.name!, currentPage, searchQuery, sortField ?? undefined, sortDirection ?? undefined);
  }
</script>


<svelte:head>
  <title>{resource?.label ? `${resource.label} | ${resource?.name}` : 'Loading...'}</title>
</svelte:head>

<div class="space-y-6">
  <div class="flex flex-col md:flex-row md:justify-between md:items-end gap-4">
    <div>
      <h2 class="text-2xl font-bold text-text-main capitalize tracking-tight">{resource?.label || params.name}</h2>
      <p class="text-xs text-text-muted mt-1 font-medium">Manage and monitor your {params.name} resource data</p>
    </div>
    
    <div class="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
      <input 
        type="text" 
        placeholder={placeholder} 
        value={searchQuery}
        on:input={handleSearch}
        class="px-4 py-2.5 bg-bg-surface border border-border-subtle rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-brand-primary/20 text-text-main"
      />
      <a href="/resources/{params.name}/create" use:link 
         class="px-5 py-2.5 bg-brand-primary text-white text-xs font-bold rounded-xl shadow-lg shadow-brand-primary/20 hover:opacity-90 transition-all text-center">
        + New
      </a>
    </div>
  </div>

  <div class="bg-bg-surface border border-border-subtle rounded-2xl shadow-sm overflow-hidden flex flex-col">
     {#if loading}
    <div class="flex flex-col items-center justify-center py-20 gap-4">
      <Loader class="w-8 h-8 text-brand-primary animate-spin" />
      <p class="text-[10px] text-text-muted font-black uppercase tracking-[0.2em]">Synchronizing...</p>
    </div>
      {:else if items.length === 0}
      <div class="p-24 text-center text-text-muted font-medium">No records found.</div>
    {:else}
      <div class="overflow-x-auto w-full">
        <table class="w-full text-left border-collapse min-w-[600px]">
          <thead class="bg-bg-main/50 border-b border-border-subtle">
            <tr>
              {#each columns as col}
                <th 
                  class="px-6 py-4 text-[10px] font-extrabold text-text-muted uppercase tracking-widest cursor-pointer hover:text-brand-primary transition-colors select-none"
                  on:click={() => handleSort(col.name)}
                >
                  <div class="flex items-center gap-1.5">
                    {col.label}
                    <span class={sortField === col.name ? "text-brand-primary font-bold" : "text-border-subtle"}>
                      {#if sortField === col.name}
                        {sortDirection === 'ASC' ? '↑' : '↓'}
                      {:else if resource?.sortableFields?.includes(col.name)}
                        ↕
                      {/if}
                    </span>
                  </div>
                </th>
              {/each}
              <th class="px-6 py-4 text-right text-[10px] font-extrabold text-text-muted uppercase tracking-widest">Action</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-border-subtle">
            {#each items as row}
              <tr class="hover:bg-bg-main/40 transition-colors group">
                {#each columns as col}
                  <td class="px-6 py-4 whitespace-nowrap text-text-main">
                    <Render type={col.type} value={row.values[col.name]} label={col.label} mode="table" />
                  </td>
                {/each}
                <td class="px-6 py-4 text-right">
                  <div class="flex justify-end gap-2">
                    <a href="/resources/{params.name}/{row.id}" use:link class="btn-secondary text-[11px] px-3 py-1.5">View</a>
                    {#if row.metadata?.canDelete}
                      <button on:click={() => handleDelete(row.id)} class="btn-danger text-[11px] px-3 py-1.5">Delete</button>
                    {/if}
                  </div>
                </td>
              </tr>
            {/each}
          </tbody>
        </table>
      </div>

      <div class="px-4 md:px-6 py-4 bg-bg-main/30 border-t border-border-subtle flex flex-col sm:flex-row items-center justify-between gap-4">
        <div class="text-[10px] text-text-muted font-bold uppercase tracking-wider">
          Showing {(currentPage - 1) * pagination.pageSize + 1} - {Math.min(currentPage * pagination.pageSize, pagination.total)} of {pagination.total}
        </div>

        <div class="flex items-center gap-2">
          <button 
            on:click={() => handlePageChange(currentPage - 1)} 
            disabled={currentPage === 1}
            class="p-2 rounded-lg border border-border-subtle bg-bg-surface text-text-main disabled:opacity-30 hover:border-brand-primary transition-colors">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" /></svg>
          </button>
          
          <span class="text-xs font-bold text-text-main px-2">
            {currentPage} <span class="text-text-muted">/</span> {pagination.totalPages}
          </span>

          <button 
            on:click={() => handlePageChange(currentPage + 1)} 
            disabled={currentPage === pagination.totalPages}
            class="p-2 rounded-lg border border-border-subtle bg-bg-surface text-text-main disabled:opacity-30 hover:border-brand-primary transition-colors">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" /></svg>
          </button>
        </div>
      </div>
    {/if}
  </div>
</div>