import { writable } from "svelte/store";

export type SnackbarType = "success" | "error" | "warning" | "info";

export interface SnackbarMessage {
    id: number;
    message: string;
    type: SnackbarType;
    duration: number;
}

const { subscribe, update } = writable<SnackbarMessage[]>([]);

function show(
    message: string,
    type: SnackbarType = "info",
    duration = 4000
) {
    const id = Date.now();

    update(items => [
        ...items,
        {
            id,
            message,
            type,
            duration
        }
    ]);

    setTimeout(() => {
        update(items => items.filter(i => i.id !== id));
    }, duration);
}

export const snackbar = {
    subscribe,
    success: (m: string) => show(m, "success"),
    error: (m: string) => show(m, "error"),
    warning: (m: string) => show(m, "warning"),
    info: (m: string) => show(m, "info")
};