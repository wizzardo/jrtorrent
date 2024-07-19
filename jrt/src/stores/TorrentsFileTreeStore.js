import {Store} from 'laco'

export const state = new Store({})

export const update = (torrent) => {
    state.set(prev => ({...prev, [torrent.hash]: torrent}));
}
export const add = (torrent) => state.set(prev => ({...prev, [torrent.hash]: torrent}))

export const remove = (torrent) => state.set(prev => {
    const next = {...prev};
    delete next[torrent.hash];
    return next;
})

export const setOpen = (torrent, path, isOpen) => state.set(prev => {
    const next = {...prev};
    let tree = next[torrent]?.tree;
    if (!tree)
        return prev

    next[torrent].tree = [...tree]
    tree = next[torrent].tree

    if (path.length === 1) {
        tree[path[0]] = {
            ...tree[path[0]],
            open: isOpen
        }
    } else {
        tree[path[0]] = {...tree[path[0]]}
        tree = tree[path[0]]
        for (let i = 1; i < path.length - 1; i++) {
            tree.children = {...tree.children}
            tree = tree?.children[path[i]]
        }
        if (tree) {
            tree.children[path[path.length - 1]] = {
                ...tree.children[path[path.length - 1]],
                open: isOpen
            }
        }
    }

    return next;
})

export const selectPath = (torrent, path) => {
    return s => {
        let tree = s[torrent]?.tree;
        if (!tree)
            return null
        tree = tree[path[0]]
        for (let i = 1; i < path.length; i++) {
            tree = tree?.children[path[i]]
        }
        return tree;
    };
}


