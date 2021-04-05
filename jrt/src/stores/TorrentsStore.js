import {Store} from 'laco'

export const state = new Store({})

export const update = (torrent) => {
    // console.log('TorrentsStore.update', torrent.hash)
    state.set(prev => ({...prev, [torrent.hash]: torrent}));
}
export const add = (torrent) => state.set(prev => ({...prev, [torrent.hash]: torrent}))

export const remove = (torrent) => state.set(prev => {
    const next = {...prev};
    delete next[torrent.hash];
    return next;
})


if (process.env.NODE_ENV !== 'production')
    window.TorrentsStore = state