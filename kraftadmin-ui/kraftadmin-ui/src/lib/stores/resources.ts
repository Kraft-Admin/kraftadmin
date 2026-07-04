import { writable } from 'svelte/store';
import type { KraftResource } from '../types/resources';

export const resourceStore = writable<KraftResource[]>([]);

// Helper to update the store from your API call
export function updateResources(newResources: KraftResource[]) {
  resourceStore.set(newResources);
}