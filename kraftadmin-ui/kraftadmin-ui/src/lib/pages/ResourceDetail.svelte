<script lang="ts">
  import { link, push } from "svelte-spa-router";
  import { Loader2, AlertCircle, ArrowLeft, Trash2, Edit3 } from "lucide-svelte";
  import Render from "../renderers/Render.svelte";
  import IconResolver from "../components/IconResolver.svelte";
  import { kraftFetch } from "../../api";
  import { snackbar } from "../stores/snackbar";
  import { confirmDialog } from "../stores/dialog";


  export let params: { name: string; id: string };


 let resource: any = null;
  let data: any = {}; 
  let values: any = {}; 
  let loading = true;
  let error: number | null = null;

  $: if (params.name || params.id) {
    loadData(params.name, params.id);
  }


// async function handleCustomAction(action: any) {
//     if (action.confirmMessage && !confirm(action.confirmMessage)) return;

//     const res = await kraftFetch(
//         `/admin/api/resources/${params.name}/id/${params.id}/action/${action.name}`,
//         {
//             method: "POST",
//             body: JSON.stringify({})
//         }
//     );

//     const response = await res.json();

//     if (res.ok && response.success !== false) { // res.ok checks for 2xx status
//         snackbar.success(response.message || `${action.label} successful.`);
//         if (action.refresh) loadData(params.name, params.id);
//     } else {
//         snackbar.error(response.message || "Action failed.");
//     }
// }

async function handleCustomAction(action: any) {

    if (action.confirmMessage) {

        const ok = await confirmDialog.open({
            title: action.label,
            message: action.confirmMessage,
            variant: "warning"
        });

        if (!ok) return;
    }

    const res = await kraftFetch(
        `/admin/api/resources/${params.name}/id/${params.id}/action/${action.name}`,
        {
            method: "POST",
            body: JSON.stringify({})
        }
    );

    const response = await res.json();

    if (res.ok && response.success !== false) {
        snackbar.success(response.message);
        if (action.refresh) {
            loadData(params.name, params.id);
        }
    } else {
        snackbar.error(response.message);
    }
}


  async function loadData(name: string, id: string) {
    loading = true;
    error = null;
    
    try {
      // ALWAYS re-fetch or re-verify metadata if the name changed
      const resMeta = await fetch("/admin/api/resources/descriptors");
      const meta = await resMeta.json();
      resource = meta.resources.find((r: any) => r.name === name);

      // Fetch Actual Data
      const resData = await fetch(`/admin/api/resources/${name}/${id}`);
      
      if (!resData.ok) {
        error = resData.status;
      } else {
        data = await resData.json();
        values = data.values || {};
      }
    } catch (e) {
      console.error("Failed to load details", e);
      error = 500;
    } finally {
      loading = false;
    }
  }

  async function handleDelete() {
    if (!confirm("Are you sure you want to delete this record?")) return;
    const res = await fetch(`/admin/api/resources/${params.name}/${params.id}`, { method: "DELETE" });
    if (res.ok) push(`/resources/${params.name}`);
    else alert("Delete failed.");
  }

 const variantClasses: Record<string, string> = {
    DEFAULT: "btn-secondary", // Mapping Enum names to your CSS classes
    PRIMARY: "btn-primary",
    DANGER: "btn-danger",
    SUCCESS: "btn-success",
    WARNING: "btn-warning",
    ERROR: "btn-danger"
  };

</script>

<div class="p-4 md:p-8 space-y-8 bg-bg-main min-h-screen transition-colors duration-300">
  {#if loading}
    <div class="flex flex-col items-center justify-center py-20 gap-4">
      <Loader2 class="w-8 h-8 text-brand-primary animate-spin" />
      <p class="text-[10px] text-text-muted font-black uppercase tracking-[0.2em]">Synchronizing...</p>
    </div>
  {:else if error}
    <div class="flex flex-col items-center justify-center py-20 gap-4">
      <AlertCircle class="w-16 h-16 text-danger/50" />
      <h2 class="text-xl font-black text-text-main uppercase tracking-widest">
        {error === 404 ? 'Resource Not Found' : 'System Error'}
      </h2>
      <a href="/resources/{params.name}" use:link class="mt-4 flex items-center gap-2 text-[10px] text-text-muted hover:text-text-main transition-colors">
        <ArrowLeft class="w-3 h-3" /> Back to list
      </a>
    </div>
  {:else if resource}
    <div class="flex flex-col md:flex-row justify-between items-start md:items-center gap-6 border-b border-border-subtle pb-8">
      <div class="space-y-1">
        <a href="/resources/{params.name}" use:link class="flex items-center gap-1 text-[10px] text-text-muted font-bold uppercase tracking-widest hover:text-brand-primary transition-colors">
          <ArrowLeft class="w-3 h-3" /> {resource.label} List
        </a>
        <h1 class="text-3xl md:text-4xl font-black text-text-main tracking-tighter">
          Record Details <span class="text-brand-primary italic opacity-50 ml-2">/</span>
          <span class="text-text-muted font-mono text-xl ml-2">#{params.id.slice(0, 8)}</span>
        </h1>
      </div>

     
      <div class="flex flex-wrap gap-3">

  {#each data.customActions as action}
    <button 
      on:click={() => handleCustomAction(action)}
      class="{variantClasses[action.variant] || 'btn-primary'} flex items-center gap-2 text-[10px] font-black uppercase tracking-widest transition-opacity hover:opacity-80"
    >
      <IconResolver name={action.icon} />
      {action.label}
    </button>
  {/each}

  <button on:click={handleDelete} class="btn-danger flex items-center gap-2 text-[10px] font-black uppercase tracking-widest">
    <Trash2 size={14} strokeWidth={2.5} /> Delete
  </button>
  <a href="/resources/{params.name}/edit/{params.id}" use:link class="btn-primary flex items-center gap-2 text-[10px] font-black uppercase tracking-widest">
    <Edit3 size={14} strokeWidth={2.5} /> Edit
  </a>
</div>


    </div>

    <div class="space-y-4">
      {#each resource.columns.filter((c: any) => c.visible !== false) as col}
        <div class="bg-bg-surface border border-border-subtle rounded-xl p-5 transition-colors">
          <div class="flex flex-col gap-3">
            <div class="flex items-center gap-2">
              <div class="w-1 h-1 rounded-full bg-brand-primary"></div>
              <span class="text-[10px] font-black text-text-muted uppercase tracking-[0.15em]">{col.label}</span>
            </div>

            <div class="text-text-main">
             <Render
    type={col.type}
    value={values[col.name]}
    relatedCollection={data.relatedResources?.[col.name]}
    label={col.label}
    mode="detail"
/>
            </div>
          </div>
        </div>
      {/each}
    </div>
  {/if}
</div>