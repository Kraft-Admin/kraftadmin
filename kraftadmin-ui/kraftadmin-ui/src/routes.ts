import Dashboard from './lib/pages/Dashboard.svelte'
import ResourceList from './lib/pages/ResourceList.svelte'
import ResourceDetail from './lib/pages/ResourceDetail.svelte'
import ResourceCreate from './lib/pages/ResourceCreate.svelte'
import NotFound from './lib/pages/NotFound.svelte'
import LogsView from './lib/pages/LogsView.svelte'
import Settings from './lib/pages/Settings.svelte'
import LoginPage from './lib/pages/LoginPage.svelte'
import {wrap} from 'svelte-spa-router/wrap'
import { authGuard } from './lib/stores/authGuard'

export const routes = {
    "/auth/login": LoginPage,

    "/": admin(Dashboard),

    "/resources/:name": admin(ResourceList),

    "/resources/:name/create": admin(ResourceCreate),

    "/resources/:name/edit/:id": admin(ResourceCreate),

    "/resources/:name/:id": admin(ResourceDetail),

    "/logs": admin(LogsView),

    "/settings": admin(Settings),

    "*": NotFound
};

function admin(component:any) {
    return wrap({
        component,
        conditions: [authGuard]
    });
}