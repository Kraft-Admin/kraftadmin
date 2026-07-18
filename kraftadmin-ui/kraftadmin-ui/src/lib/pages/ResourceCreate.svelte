<script lang="ts">
	import { push } from 'svelte-spa-router';
	import DynamicForm from '../components/DynamicForm.svelte';
	import { notify } from '../toast';
	import { flash } from '../stores/flash';
	import { snackbar } from '../stores/snackbar';

	import type {
		DescriptorsResponse,
		KraftAdminResource,
		KraftOperationResponse,
		ResourceRow
	} from '../types/resources';
  import { Loader } from 'lucide-svelte';

	// Types

	interface RouteParams {
		name?: string;
		id?: string;
	}

	/**
	 * Form data is the ResourceRow's values flattened with
	 * top-level metadata such as id.
	 */
	type ResourceFormData = {
		id?: string;
		[key: string]: unknown;
	};

	// Props

	export let params: RouteParams = {};

	// State

	let descriptor: KraftAdminResource | null = null;

	// let initialData: ResourceFormData | null = null;
	let initialData: ResourceRow | null = null;

	let loading = true;

	let isInitialized = false;

	let serverErrors: Record<string, string[]> = {};

	/**
	 * Tracks the last resource/id combination fetched.
	 * Prevents unnecessary duplicate fetches caused by
	 * Svelte reactive updates.
	 */
	let lastFetchedKey = '';

	// Reactive Fetch

	$: {
		const key = `${params.name}__${params.id ?? 'new'}`;

		if (params.name && key !== lastFetchedKey) {
			lastFetchedKey = key;
			fetchData(params.name, params.id);
		}
	}

	// Fetch Resource Metadata + Existing Data

	async function fetchData(name: string, id?: string): Promise<void> {
		if (!isInitialized) {
			loading = true;
		}

		try {
			// Fetch descriptors

			const resMeta = await fetch(
				'/admin/api/resources/descriptors'
			);

			if (!resMeta.ok) {
				throw new Error(
					`Failed to load descriptors: ${resMeta.status}`
				);
			}

			const meta: DescriptorsResponse = await resMeta.json();

			descriptor =
				meta.resources.find(
					(resource: KraftAdminResource) =>
						resource.name === name
				) ?? null;

			// Fetch existing resource

			if (id) {
				const resData = await fetch(
					`/admin/api/resources/${name}/${id}`
				);

				if (!resData.ok) {
					throw new Error(
						`Failed to load resource: ${resData.status}`
					);
				}

				const response: KraftOperationResponse<ResourceRow> =
					await resData.json();

				if (response.success && response.data) {
					const flattenedData: ResourceFormData = {
						id: response.data.id,
						...response.data.values
					};

					console.log(
						'Flattened data for form:',
						flattenedData
					);

				
					if (
	JSON.stringify(response.data) !==
	JSON.stringify(initialData)
) {
	initialData = response.data;
}
				}
			} else if (!isInitialized) {
				initialData = null;
			}

			isInitialized = true;
		} catch (error) {
			console.error('Fetch error:', error);

			notify(
				'Error loading resource configuration'
			);
		} finally {
			loading = false;
		}
	}

	// Submit

	async function handleSubmit(
		formData: Record<string, unknown>
	): Promise<void> {
		const isEdit = Boolean(params.id);

		console.log('Submitting', formData);

		serverErrors = {};

		try {
			const res = await fetch(
				`/admin/api/resources/${params.name}`,
				{
					method: 'POST',
					headers: {
						'Content-Type': 'application/json'
					},
					body: JSON.stringify(formData)
				}
			);

			const result: KraftOperationResponse<unknown> =
				await res.json();

			if (res.ok && result.success) {
				flash.success(
					result.message ??
						`${params.name} ${
							isEdit ? 'updated' : 'created'
						} successfully.`
				);

				push(`/resources/${params.name}`);

				return;
			}

			if (result.errors) {
				serverErrors = result.errors;
			}

			snackbar.error(
				result.message ?? 'Operation failed.'
			);
		} catch (error) {
			console.error('Submit error:', error);

			snackbar.error(
				'Unexpected server error.'
			);
		}
	}
</script>

<div class="space-y-6">
	<div class="flex items-center gap-4">
		<button
			on:click={() => window.history.back()}
			class="p-2 -ml-2 rounded-full hover:bg-bg-surface text-zinc-400 hover:text-brand-primary transition-all duration-200"
		>
			<svg
				xmlns="http://www.w3.org/2000/svg"
				class="w-6 h-6"
				fill="none"
				viewBox="0 0 24 24"
				stroke="currentColor"
			>
				<path
					stroke-linecap="round"
					stroke-linejoin="round"
					stroke-width="2"
					d="M10 19l-7-7m0 0l7-7m-7 7h18"
				/>
			</svg>

			<title>Back</title>
		</button>

		<div>
			<h2 class="text-2xl font-bold text-text-main capitalize">
				{params.id ? 'Edit' : 'New'} {params.name}
			</h2>

			<p class="text-xs text-zinc-500 font-medium">
				{params.id
					? `Modifying existing ${params.name}`
					: `Create a new ${params.name}`}
			</p>
		</div>
	</div>

	{#if loading && !isInitialized}
		 <div class="flex flex-col items-center justify-center py-20 gap-4">
        <Loader class="w-8 h-8 text-brand-primary animate-spin" />
        <p class="text-[10px] text-text-muted font-black uppercase tracking-[0.2em]">Loading...</p>
      </div>

	{:else if descriptor}
		<DynamicForm
			columns={descriptor.columns}
			initialData={initialData}
			onSubmit={handleSubmit}
			resourceName={params.name ?? ''}
			externalErrors={serverErrors}
		/>

	{:else}
		<div
			class="p-12 text-center bg-bg-surface border border-dashed border-border-subtle rounded-xl"
		>
			<p class="text-zinc-500">
				Resource not found.
			</p>
		</div>
	{/if}
</div>