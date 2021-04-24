import React, {useEffect, useState, useRef} from 'react';
import './TorrentFileTree.css'
import {useStore} from '../stores/StoreUtils'
import {state} from "../stores/TorrentsFileTreeStore";
import API from "../network/API";
import {TorrentFileTreeEntry} from "./TorrentFileTreeEntry";
import Scrollable,{SCROLLBAR_MODE_AUTO, SCROLLBAR_MODE_HIDDEN} from "react-ui-basics/Scrollable";
import "react-ui-basics/Scrollable.css";

export default React.memo(({hash, show}) => {
    const data = useStore(state, it => it[hash])
    const [totalChildrenToShow, setTotalChildrenToShow] = useState(0)
    const [totalChildrenHidden, setTotalChildrenHidden] = useState(0)

    const totalChildrenToShowRef = useRef()
    totalChildrenToShowRef.current = totalChildrenToShow;

    useEffect(() => {
        if (!data && show) {
            API.getWs().send('GetTorrentFileTree', {hash})
        }
    }, [hash, show])

    useEffect(() => {
        if (data)
            if (!show) {
                setTotalChildrenHidden(totalChildrenToShow);
                setTotalChildrenToShow(0)
            } else if (!totalChildrenToShowRef.current) {
                setTotalChildrenToShow(totalChildrenHidden || data && data.tree.length);
            }
    }, [show, data])

    return <div className="TorrentFileTree">
        <div className="resizeable" style={{
            height: (35 * totalChildrenToShow || 0) + 'px'
        }}>
            <Scrollable horizontalScrollBarMode={SCROLLBAR_MODE_AUTO} scrollBarMode={SCROLLBAR_MODE_HIDDEN}>
                {data && data.tree.map(it => <TorrentFileTreeEntry {...it} open={true} hash={hash} key={it.id} parentPath={() => ''} updateParentShownChildrenCount={(innerChilds) => {
                    if (typeof (innerChilds) != "undefined") {
                        let toShow = (totalChildrenToShow || data.tree.length) + innerChilds;
                        setTotalChildrenToShow(toShow)
                        totalChildrenToShowRef.current = toShow;
                    }
                }}/>)}
            </Scrollable>
        </div>
    </div>
})