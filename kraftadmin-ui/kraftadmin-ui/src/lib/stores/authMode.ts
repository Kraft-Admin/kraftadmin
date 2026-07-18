import { writable } from 'svelte/store';

export type AuthMode = 'bridge' | 'standalone' | 'unknown';

export const authMode = writable<AuthMode>('unknown');
export const isBridgeMode = writable(false);