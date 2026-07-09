import { writable } from "svelte/store";

type DialogOptions = {
    title: string;
    message: string;
    variant?: "info" | "warning" | "danger";
    confirmText?: string;
    cancelText?: string;
};

let resolver: ((value: boolean) => void) | null = null;

export const dialog = writable<DialogOptions | null>(null);

export const confirmDialog = {
    open(options: DialogOptions): Promise<boolean> {
        dialog.set(options);

        return new Promise(resolve => {
            resolver = resolve;
        });
    },

    confirm() {
        dialog.set(null);
        resolver?.(true);
    },

    cancel() {
        dialog.set(null);
        resolver?.(false);
    }
};