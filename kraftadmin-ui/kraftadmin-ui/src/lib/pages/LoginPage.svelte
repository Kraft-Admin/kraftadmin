<script lang="ts">
    import { replace } from "svelte-spa-router";
    import Login from "../../lib/components/Login.svelte";

    function success() {
        replace("/");
    }
</script>

<div>
<Login on:success={success}/>
</div>
