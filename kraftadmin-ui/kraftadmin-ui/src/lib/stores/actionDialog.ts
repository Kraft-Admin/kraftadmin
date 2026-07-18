import { writable, get } from "svelte/store";

type DialogState = {
    open: boolean;
    action: any | null;
};

const state = writable<DialogState>({
    open: false,
    action: null
});

let resolver: ((value: any | null) => void) | null = null;

export const actionDialog = {

    subscribe: state.subscribe,

    open(action: any): Promise<any | null> {

        state.set({
            open: true,
            action
        });

        return new Promise(resolve => {
            resolver = resolve;
        });
    },

    submit(data: any) {

        resolver?.(data);
        resolver = null;

        state.set({
            open: false,
            action: null
        });
    },

    cancel() {

        resolver?.(null);
        resolver = null;

        state.set({
            open: false,
            action: null
        });
    }
};