import { replace } from "svelte-spa-router";
import { kraftFetch } from "../../api";

export async function authGuard() {
    console.log("[Guard] Checking authentication");

    const res = await kraftFetch("/admin/api/resources/descriptors");

    console.log("[Guard] Status:", res.status);

    if (res.status === 401) {
        console.log("[Guard] Redirecting to login");
        replace("/auth/login");
        return false;
    }

    console.log("[Guard] Access granted");
    return true;
}