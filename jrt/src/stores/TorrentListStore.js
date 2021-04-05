import {Store} from 'laco'
import * as TorrentsStore from "./TorrentsStore";

const name = 'TorrentListStore'

export const state = new Store({
    list: [],
}, name)

export const update = ({torrents}) => {
    // console.log(name, 'update', torrents)
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
    window[name] = state
