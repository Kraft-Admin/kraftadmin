<script lang="ts">
  import SmartValueRenderer from "./SmartValueRenderer.svelte"; // self-import for recursion

  export let value: any;
  export let label: string | null = null;
  export let depth: number = 0;

  function isPlainObject(v: any): boolean {
    return v !== null && typeof v === "object" && !Array.isArray(v);
  }
  function isObjectResponse(v: any): boolean {
    return isPlainObject(v) && "id" in v && "label" in v;
  }
  function isEmbeddedResponse(v: any): boolean {
    return isPlainObject(v) && "data" in v && "summary" in v;
  }
  function isMapRow(v: any): boolean {
    return isPlainObject(v) && "key" in v && "value" in v && Object.keys(v).length === 2;
  }
  function isImagePath(v: any): boolean {
    return typeof v === "string" && /\.(jpe?g|png|webp|gif|svg)$/i.test(v.trim());
  }
  function isUrl(v: any): boolean {
    return typeof v === "string" && /^https?:\/\//i.test(v);
  }
  function isIsoDate(v: any): boolean {
    return typeof v === "string" && /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/.test(v);
  }
  function isScalar(v: any): boolean {
    return v === null || v === undefined || ["string", "number", "boolean"].includes(typeof v);
  }

  type Kind =
    | "empty" | "boolean" | "number" | "string" | "url" | "image" | "date"
    | "relation" | "embedded" | "object"
    | "map-array" | "relation-array" | "image-array" | "tag-array" | "object-array";

  function resolveKind(v: any): Kind {
    if (v === null || v === undefined || v === "") return "empty";

    if (Array.isArray(v)) {
      if (v.length === 0) return "empty";
      if (v.every(isMapRow)) return "map-array";
      if (v.every(isObjectResponse)) return "relation-array";
      if (v.every(isImagePath)) return "image-array";
      if (v.every((x) => x === null || ["string", "number", "boolean"].includes(typeof x))) return "tag-array";
      return "object-array";
    }

    if (isObjectResponse(v)) return "relation";
    if (isEmbeddedResponse(v)) return "embedded";
    if (isPlainObject(v)) return "object";

    if (typeof v === "boolean") return "boolean";
    if (typeof v === "number") return "number";
    if (typeof v === "string") {
      if (isImagePath(v)) return "image";
      if (isUrl(v)) return "url";
      if (isIsoDate(v)) return "date";
      return "string";
    }
    return "string";
  }

  function humanize(key: string): string {
    return key.replace(/([a-z0-9])([A-Z])/g, "$1 $2").replace(/^./, (c) => c.toUpperCase());
  }

  function formatDate(v: string): string {
    const d = new Date(v);
    return isNaN(d.getTime()) ? v : d.toLocaleString();
  }

  function unwrap(v: any): any {
    return isEmbeddedResponse(v) ? v.data : v;
  }

  const TITLE_KEYS = /^(name|title|label|street)$/i;
  const SUBTITLE_KEYS = /(description|summary|city|country|number|email)/i;

  function pickCard(obj: Record<string, any>) {
    const entries = Object.entries(obj).map(([k, v]) => [k, unwrap(v)] as [string, any]);

    let image: string | null = null;
    for (const [, v] of entries) {
      if (typeof v === "string" && isImagePath(v)) { image = v; break; }
    }

    let titleKey: string | null = null;
    let title: string | null = null;
    for (const [k, v] of entries) {
      if (typeof v === "string" && v.trim() && TITLE_KEYS.test(k)) { titleKey = k; title = v; break; }
    }
    if (!title) {
      for (const [k, v] of entries) {
        if (typeof v === "string" && v.trim() && !isImagePath(v) && !isUrl(v)) { titleKey = k; title = v; break; }
      }
    }

    let subtitleKey: string | null = null;
    let subtitle: string | null = null;
    for (const [k, v] of entries) {
      if (k === titleKey) continue;
      if (typeof v === "string" && v.trim() && !isImagePath(v)) {
        if (!subtitle) { subtitleKey = k; subtitle = v; }
        if (SUBTITLE_KEYS.test(k)) { subtitleKey = k; subtitle = v; break; }
      }
    }

    let metricKey: string | null = null;
    let metric: number | null = null;
    for (const [k, v] of entries) {
      if (typeof v === "number") { metricKey = k; metric = v; break; }
    }

    const used = new Set([titleKey, subtitleKey, metricKey].filter(Boolean) as string[]);
    const rest = entries.filter(([k, v]) => !used.has(k) && !(typeof v === "string" && v === image));

    const simpleRest = rest.filter(([, v]) => isScalar(v) && v !== "" && v !== null);
    const complexRest = rest.filter(([, v]) => !isScalar(v) && v !== null && !(Array.isArray(v) && v.length === 0));

    return { image, title, subtitle, metricKey, metric, simpleRest, complexRest };
  }

  const AVATAR_COLORS = ["#6366f1", "#f97316", "#0ea5e9", "#22c55e", "#a855f7", "#ec4899"];
  function avatarColor(seed: string): string {
    let h = 0;
    for (let i = 0; i < seed.length; i++) h = (h * 31 + seed.charCodeAt(i)) >>> 0;
    return AVATAR_COLORS[h % AVATAR_COLORS.length];
  }

  $: kind = resolveKind(value);
</script>

<div class="smart-value" class:pl-3={depth > 0} class:border-l={depth > 0} class:border-border-subtle={depth > 0}>
  {#if kind === "empty"}
    <span class="text-text-muted italic">—</span>

  {:else if kind === "boolean"}
    <span class="chip {value ? 'chip-success' : 'chip-neutral'}">{value ? "Yes" : "No"}</span>

  {:else if kind === "number"}
    <span class="font-mono text-text-main">{value}</span>

  {:else if kind === "date"}
    <span class="text-text-main">{formatDate(value)}</span>

  {:else if kind === "url"}
    <a href={value} target="_blank" rel="noopener" class="text-brand-primary underline break-all">{value}</a>

  {:else if kind === "image"}
    <img src={value} alt={label ?? ""} class="h-16 w-16 object-cover rounded-lg border border-border-subtle" />

  {:else if kind === "string"}
    <span class="text-text-main whitespace-pre-wrap break-words">{value}</span>

  {:else if kind === "relation"}
    <span class="chip chip-relation" title={value.id}>{value.label}</span>

  {:else if kind === "embedded"}
    <svelte:self value={value.data} label={value.summary || label} depth={depth + 1} />

  {:else if kind === "object"}
    <div class="card p-3 space-y-2">
      {#each Object.entries(value) as [k, v] (k)}
        <div class="grid grid-cols-1 md:grid-cols-3 gap-2 items-start">
          <span class="field-label">{humanize(k)}</span>
          <div>
            <svelte:self value={v} label={humanize(k)} depth={depth + 1} />
          </div>
        </div>
      {/each}
    </div>

  {:else if kind === "relation-array"}
    <div class="flex flex-wrap gap-1.5">
      {#each value as item (item.id)}
        <span class="chip chip-relation" title={item.id}>{item.label}</span>
      {/each}
    </div>

  {:else if kind === "image-array"}
    <div class="flex flex-wrap gap-2">
      {#each value as src, i (src + i)}
        <img {src} alt="" class="h-14 w-14 object-cover rounded-lg border border-border-subtle" />
      {/each}
    </div>

  {:else if kind === "tag-array"}
    <div class="flex flex-wrap gap-1.5">
      {#each value as item, i (i)}
        <span class="chip chip-neutral">{item}</span>
      {/each}
    </div>

 {:else if kind === "map-array"}
    <div class="card divide-y divide-border-subtle">
      {#each value as row, i (i)}
        <!-- Changed flex items-start gap-3 to flex-col md:flex-row -->
        <div class="flex flex-col md:flex-row items-start gap-1 md:gap-3 px-3 py-2">
          <span class="chip chip-key shrink-0">{row.key}</span>
          <div class="flex-1 min-w-0 w-full">
            <svelte:self value={row.value} depth={depth + 1} />
          </div>
        </div>
      {/each}
    </div>

  {:else if kind === "object-array"}
    <div class="obj-card-list">
      {#each value as item, i (i)}
        {@const card = pickCard(item)}
        <div class="obj-card">
          <div class="obj-card-media">
            {#if card.image}
              <img src={card.image} alt={card.title ?? ""} />
            {:else}
              <div class="obj-card-avatar" style="background:{avatarColor(card.title ?? String(i))}">
                {(card.title ?? "#" + (i + 1)).charAt(0).toUpperCase()}
              </div>
            {/if}
          </div>

          <div class="obj-card-body">
            <div class="obj-card-title">{card.title ?? `Item ${i + 1}`}</div>
            {#if card.subtitle}
              <div class="obj-card-subtitle">{card.subtitle}</div>
            {/if}

            {#if card.simpleRest.length}
              <div class="obj-card-meta justify-center md:justify-start">
                {#each card.simpleRest as [k, v] (k)}
                  <span class="meta-chip"><span class="meta-key">{humanize(k)}</span>{v}</span>
                {/each}
              </div>
            {/if}

            {#if card.complexRest.length}
              <div class="obj-card-nested">
                {#each card.complexRest as [k, v] (k)}
                  <div class="obj-card-nested-row">
                    <span class="field-label !mb-0">{humanize(k)}</span>
                    <svelte:self value={v} label={humanize(k)} depth={depth + 1} />
                  </div>
                {/each}
              </div>
            {/if}
          </div>

          {#if card.metric !== null}
            <div class="obj-card-metric">{card.metric}</div>
          {/if}
        </div>
      {/each}
    </div>
  {/if}
</div>