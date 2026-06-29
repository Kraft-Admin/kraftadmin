import { writable } from 'svelte/store';

// We start false. The App.svelte component's onMount check will 
// flip this to true if the session cookie is valid.
export const isAuthenticated = writable(false);
export const user = writable<{ username: string; roles: string[] } | null>(null);

export function clearAuth() {
    user.set(null);
    isAuthenticated.set(false);
}