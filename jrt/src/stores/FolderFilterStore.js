import {Store} from 'laco'

export const state = new Store({folder: 'ALL'})

export const FOLDER_FILTER_ALL = 'ALL'
export const FOLDER_FILTER_DEFAULT = 'default'
export const FOLDERS = [FOLDER_FILTER_DEFAULT, 'movies', 'series', 'music', 'games', 'other']

export const update = (folder) => {
    state.set(prev => ({folder}));
}

