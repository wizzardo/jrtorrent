import {Store} from 'laco'

export const state = new Store({
    show: false
})

export const show = ({title, description, accept, onAccept, cancel, onCancel, buttons}) => {
    state.set(prev => ({
        show: true,
        accept,
        cancel,
        onAccept,
        title,
        description,
        buttons,
        onCancel: onCancel && (() => {
            onCancel && onCancel()
            hide()
        })
    }));
}
export const hide = () => {
    state.set(prev => ({
        show: false,
        accept: null,
        cancel: null,
        onAccept: null,
        title: null,
        description: null,
        buttons: null,
        onCancel: null
    }));
}
