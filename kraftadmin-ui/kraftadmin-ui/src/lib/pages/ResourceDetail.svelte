<script lang="ts">
	import { link, push } from 'svelte-spa-router';
	import {
		Loader2,
		AlertCircle,
		ArrowLeft,
		Trash2,
		Edit3
	} from 'lucide-svelte';

	import Render from '../renderers/Render.svelte';
	import IconResolver from '../components/IconResolver.svelte';

	import { kraftFetch } from '../../api';
	import { snackbar } from '../stores/snackbar';
	import { confirmDialog } from '../stores/dialog';
	import { actionDialog } from '../stores/actionDialog';

	import type {
		ActionVariant,
		CustomAction,
		DescriptorsResponse,
		KraftAdminResource,
		KraftOperationResponse,
		ResourceRow
	} from '../types/resources';

	// Types

	interface RouteParams {
		name: string;
		id: string;
	}

	// Props

	export let params: RouteParams;

	// State

	let resource: KraftAdminResource | null = null;

	let data: ResourceRow | null = null;

	let values: Record<string, unknown> = {};

	let loading = true;

	let error: number | null = null;

	// Action Styles

	const variantClasses: Record<ActionVariant, string> = {
		DEFAULT: 'btn-secondary',
		PRIMARY: 'btn-primary',
		SUCCESS: 'btn-success',
		WARNING: 'btn-warning',
		DANGER: 'btn-danger',
		ERROR: 'btn-danger'
	};

	// Load Data

	$: if (params.name && params.id) {
		loadData(params.name, params.id);
	}

	async function loadData(
		name: string,
		id: string
	): Promise<void> {
		loading = true;
		error = null;

		try {
			// Fetch resource descriptors

			const metaRes = await fetch(
				'/admin/api/resources/descriptors'
			);

			if (!metaRes.ok) {
				throw new Error(
					`Failed to load descriptors: ${metaRes.status}`
				);
			}

			const meta: DescriptorsResponse =
				await metaRes.json();

			resource =
				meta.resources.find(
					(item: KraftAdminResource) =>
						item.name === name
				) ?? null;

			// Fetch resource record

			const dataRes = await fetch(
				`/admin/api/resources/${name}/${id}`
			);

			const response: KraftOperationResponse<ResourceRow> =
				await dataRes.json();

			if (!dataRes.ok || !response.success) {
				error =
					dataRes.status === 200
						? 404
						: dataRes.status;

				snackbar.error(
					response.message ??
						'An unexpected error occurred.'
				);

				return;
			}

			if (!response.data) {
				error = 404;

				snackbar.error(
					'Resource record was not found.'
				);

				return;
			}

			data = response.data;

			values = data.values ?? {};
		} catch (e) {
			console.error('Failed to load resource:', e);

			error = 500;

			snackbar.error(
				'Failed to connect to the server.'
			);
		} finally {
			loading = false;
		}
	}

	// Execute Custom Action

	async function executeAction(
		action: CustomAction,
		input: Record<string, unknown> = {}
	): Promise<void> {
		const res = await kraftFetch(
			`/admin/api/resources/${params.name}/id/${params.id}/action/${action.name}`,
			{
				method: 'POST',
				body: JSON.stringify(input)
			}
		);

		const response: KraftOperationResponse<unknown> =
			await res.json();

		if (res.ok && response.success) {
			snackbar.success(
				response.message ??
					`${action.label} completed successfully.`
			);

			if (action.refresh) {
				await loadData(
					params.name,
					params.id
				);
			}

			return;
		}

		snackbar.error(
			response.message ??
				'Action execution failed.'
		);
	}

	// Handle Custom Action

	async function handleCustomAction(
		action: CustomAction
	): Promise<void> {
		// Confirmation

		if (action.confirmMessage) {
			const confirmed =
				await confirmDialog.open({
					title: action.label,
					message: action.confirmMessage,
					variant: 'warning'
				});

			if (!confirmed) {
				return;
			}
		}

		// Action Input

		let input: Record<string, unknown> = {};

		if (action.input) {
			const result =
				await actionDialog.open(action);

			if (result == null) {
				return;
			}

			input = result;
		}

		await executeAction(action, input);
	}

	// Delete

	async function handleDelete(): Promise<void> {
		const confirmed =
			await confirmDialog.open({
				title: 'Delete Record',
				message:
					'Are you sure you want to delete this record?',
				variant: 'danger'
			});

		if (!confirmed) {
			return;
		}

		const res = await kraftFetch(
			`/admin/api/resources/${params.name}/${params.id}`,
			{
				method: 'DELETE'
			}
		);

		const response: KraftOperationResponse<unknown> =
			await res.json();

		if (res.ok && response.success) {
			snackbar.success(
				response.message ??
					'Record deleted.'
			);

			push(
				`/resources/${params.name}`
			);

			return;
		}

		snackbar.error(
			response.message ??
				'Delete failed.'
		);
	}
</script>

<div class="p-4 md:p-8 space-y-8 bg-bg-main min-h-screen">
	{#if loading}
		<div
			class="flex flex-col items-center justify-center py-20 gap-4"
		>
			<Loader2
				class="w-8 h-8 text-brand-primary animate-spin"
			/>

			<p
				class="text-[10px] font-black uppercase tracking-[0.2em] text-text-muted"
			>
				Synchronizing...
			</p>
		</div>

	{:else if error}
		<div
			class="flex flex-col items-center justify-center py-20 gap-4"
		>
			<AlertCircle
				class="w-16 h-16 text-danger/50"
			/>

			<h2
				class="text-xl font-black uppercase tracking-widest"
			>
				{error === 404
					? 'Resource Not Found'
					: 'System Error'}
			</h2>

			<a
				href="/resources/{params.name}"
				use:link
				class="mt-4 flex items-center gap-2 text-[10px] uppercase font-bold tracking-widest text-text-muted hover:text-text-main"
			>
				<ArrowLeft class="w-3 h-3" />

				Back to list
			</a>
		</div>

	{:else if resource && data}
		<div
			class="flex flex-col md:flex-row justify-between items-start md:items-center gap-6 border-b border-border-subtle pb-8"
		>
			<div class="space-y-1">
				<a
					href="/resources/{params.name}"
					use:link
					class="flex items-center gap-1 text-[10px] uppercase font-bold tracking-widest text-text-muted hover:text-brand-primary"
				>
					<ArrowLeft class="w-3 h-3" />

					{resource.label} List
				</a>

				<h3
					class="text-2xl font-black tracking-tight text-text-muted"
				>
					Record Details

					<span
						class="text-brand-primary italic opacity-50 ml-2"
					>
						/
					</span>
				</h3>
			</div>

			<div class="flex flex-wrap gap-3">
				{#each data.customActions ?? [] as action}
					<button
						class="{variantClasses[action.variant]} flex items-center gap-2 text-[10px] uppercase font-black tracking-widest"
						on:click={() =>
							handleCustomAction(action)}
					>
						<IconResolver
							name={action.icon}
						/>

						{action.label}
					</button>
				{/each}

				<button
					class="btn-danger flex items-center gap-2 text-[10px] uppercase font-black tracking-widest"
					on:click={handleDelete}
				>
					<Trash2 size={14} />

					Delete
				</button>

				<a
					href="/resources/{params.name}/edit/{params.id}"
					use:link
					class="btn-primary flex items-center gap-2 text-[10px] uppercase font-black tracking-widest"
				>
					<Edit3 size={14} />

					Edit
				</a>
			</div>
		</div>

		<div class="space-y-4">
			{#each resource.columns as col}
				<div
					class="bg-bg-surface border border-border-subtle rounded-xl p-5"
				>
					<div class="flex flex-col gap-3">
						<div class="flex items-center gap-2">
							<div
								class="w-1 h-1 rounded-full bg-brand-primary"
							/>

							<span
								class="text-[10px] uppercase font-black tracking-[0.15em] text-text-muted"
							>
								{col.label}
							</span>
						</div>

						<Render
							type={col.type}
							value={values[col.name]}
							relatedCollection={
								data.relatedResources?.[col.name]
							}
							elementCollection={
								col.elementCollection
							}
							label={col.label}
							mode="detail"
						/>
					</div>
				</div>
			{/each}
		</div>
	{/if}
</div>