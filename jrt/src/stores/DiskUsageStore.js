import {Store} from 'laco'

export const state = new Store({free: 0})

export const update = ({free}) => state.set(prev => ({free}))

