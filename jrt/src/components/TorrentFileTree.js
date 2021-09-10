import React, {useEffect, useState, useRef} from 'react';
import './TorrentFileTree.css'
import {useStore} from '../stores/StoreUtils'
import {state} from "../stores/TorrentsFileTreeStore";
import API from "../network/API";
import {TorrentFileTreeEntry, countOpenEntries} from "./TorrentFileTreeEntry";
import Scrollable, {SCROLLBAR_MODE_AUTO, SCROLLBAR_MODE_HIDDEN} from "react-ui-basics/Scrollable";
import "react-ui-basics/Scrollable.css";

const countOpen = (tree) => {
    if (!tree) return 0
    let count = 0
    for (let i = 0; i < tree.length; i++) {
        count++;
        if (tree[i].open)
            count += countOpenEntries(tree[i].children)
    }
    return count
}

export default ({hash, show, folder}) => {
    const tree = useStore(state, it => it[hash]?.tree)

    useEffect(() => {
        if (!tree && show) {
            API.getWs().send('GetTorrentFileTree', {hash})
        }
    }, [hash, show])

    const totalChildrenToShow = show ? countOpen(tree) : 0
    // console.log("TorrentFileTree", totalChildrenToShow, tree)
    return <div className="TorrentFileTree">
        <div className="resizeable" style={{
            height: (35 * totalChildrenToShow || 0) + 'px'
        }}>
            <Scrollable horizontalScrollBarMode={SCROLLBAR_MODE_AUTO} scrollBarMode={SCROLLBAR_MODE_HIDDEN}>
                {show && tree && tree.map((it, i) => <TorrentFileTreeEntry forceOpen={true}
                                                                           hash={hash}
                                                                           key={it.id}
                                                                           path={[i]}
                                                                           parentPath={folder ? '/' + folder : ''}
                />)}
            </Scrollable>
        </div>
    </div>
}