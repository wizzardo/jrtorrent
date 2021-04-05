import {Store} from 'laco'
import * as TorrentsStore from "./TorrentsStore";

export const state = new Store({
    list: [],
})

export const update = ({torrents}) => {
    torrents.forEach(it => TorrentsStore.add(it))
    state.set(prev => ({list: torrents.map(it => it.hash)}));
}

export const remove = (hash) => {
    state.set(({list}) => ({list: list.filter(it => it !== hash)}));
}
export const add = (hash) => {
    if (!state.get().list.includes(hash))
        state.set(({list}) => ({list: [hash, ...list]}));
}

if (process.env.NODE_ENV !== 'production')
    window.TorrentListStore = state
