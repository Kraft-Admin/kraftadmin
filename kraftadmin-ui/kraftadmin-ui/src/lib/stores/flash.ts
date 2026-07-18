const KEY = "kraftadmin.flash";

export interface FlashMessage {
    type: "success" | "error" | "warning" | "info";
    message: string;
}

export const flash = {

    success(message: string) {
        this.store({
            type: "success",
            message
        });
    },

    error(message: string) {
        this.store({
            type: "error",
            message
        });
    },

    warning(message: string) {
        this.store({
            type: "warning",
            message
        });
    },

    info(message: string) {
        this.store({
            type: "info",
            message
        });
    },

    store(message: FlashMessage) {
        sessionStorage.setItem(KEY, JSON.stringify(message));
    },

    consume(): FlashMessage | null {

        const raw = sessionStorage.getItem(KEY);

        if (!raw) {
            return null;
        }

        sessionStorage.removeItem(KEY);

        try {
            return JSON.parse(raw);
        } catch {
            return null;
        }
    }
};