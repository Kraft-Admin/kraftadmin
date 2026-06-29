import { replace } from "svelte-spa-router";
import { isAuthenticated } from "./lib/stores/auth";

export async function kraftFetch(path: string, options: RequestInit = {}) {

    const headers = new Headers(options.headers || {});

    headers.set("Accept", "application/json");

    if (!(options.body instanceof FormData)) {
        headers.set("Content-Type", "application/json");
    }

    const response = await fetch(path, {
        ...options,
        headers,
        credentials: "same-origin"
    });


    if (response.status === 401 || response.status === 403) {


        isAuthenticated.set(false);


        replace("/auth/login");


        return response;  
    }

    return response;
}