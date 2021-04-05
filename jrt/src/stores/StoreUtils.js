import {useEffect, useState} from "react";

const defaultSelector = store => store;

export function useStore(store, selector = defaultSelector) {
    const [state, setState] = useState(selector(store.get()))

    function updateState() {
        setState(selector(store.get()))
    }

    useEffect(() => {
        store.subscribe(updateState)
        return () => store.unsubscribe(updateState)
    })

    return state
}