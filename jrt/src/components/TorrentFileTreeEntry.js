import React, {useEffect, useRef, useState} from 'react';
import './TorrentFileTreeEntry.css'
import AutocompleteSelect from "react-ui-basics/AutocompleteSelect";
import "react-ui-basics/AutocompleteSelect.css";
import "react-ui-basics/FilteredList.css";
import "react-ui-basics/Scrollable.css";
import {SCROLLBAR_MODE_HIDDEN} from "react-ui-basics/Scrollable";
import {preventDefault, stopPropagation} from "react-ui-basics/Tools";
import API from "../network/API";
import * as TorrentsBitfieldStore from "../stores/TorrentsBitfieldStore";
import {CircleProgress} from "react-ui-basics";
import {useStore} from "../stores/StoreUtils";
import 'react-ui-basics/CircleProgress.css'
import {Size} from "react-ui-basics";

const openUrl = (url, e) => {
    e.processed = true;
    preventDefault(e);
    stopPropagation(e);
    window.open(url, '_blank');
}

const openLink = e => {
    let target = e.target;
    while (target && !target.href) {
        target = target.parentNode;
    }

    target && openUrl(target.href, e)
    return false;
}

const countProgress = (bitfield, offset, length) => {
    if (!bitfield)
        return 0

    let completed = 0
    const end = offset + length;
    for (let i = offset; i < end; i++) {
        if (bitfield.has(i))
            completed++;
    }
    return (completed * 100 / length).toFixed(2);
}

export const TorrentFileTreeEntry = (props) => {
    const {
        children,
        // chunksCompleted,
        // chunksCount,
        id,
        isFolder,
        name,
        priority,
        updateParentShownChildrenCount,
        hidden,
        parentPath,
        hash,
        piecesOffset,
        piecesLength,
        sizeBytes,
        open
    } = props;
    const bitfield = useStore(TorrentsBitfieldStore.state, s => s[hash])

    const [showChildren, setShowChildren] = useState(false)
    const [shownChildren, setShownChildren] = useState(0)
    const [hiddenChildren, setHiddenChildren] = useState(children && Object.values(children).length || 0)

    const updateShownChildrenCount = innerChilds => {
        // console.log('entry.updateShownChildsCount ' + innerChilds + ' in ' + name);
        if (typeof (innerChilds) != "undefined")
            setShownChildren(shownChildren + innerChilds)

        if (updateParentShownChildrenCount)
            updateParentShownChildrenCount(innerChilds);
    }

    const toggleChildren = (e) => {
        if (e.processed)
            return;
        e.processed = true;
        setShowChildren(!showChildren);

        var t = shownChildren;
        updateShownChildrenCount(!showChildren ? hiddenChildren : -shownChildren);
        setHiddenChildren(t)
    };

    useEffect(() => !!open && toggleChildren({}), [!!open])

    const path = () => parentPath() + '/' + encodeURIComponent(name)

    const [progress, setProgress] = useState(0);
    useEffect(() => {
        if (!hidden) {
            if (!isFolder)
                setProgress(countProgress(bitfield, piecesOffset, piecesLength));
        } else {
            setProgress(0)
        }
    }, [hidden, bitfield])

    return <div className="TorrentFileTreeEntry" onClick={toggleChildren}>
        <div className="info">
            {isFolder && <div className="folder">
                <i className="material-icons">{showChildren ? 'folder_open' : 'folder'}</i>
                <span className="folderName">{name}</span>

                <a href={!hidden && API.zipLink(path())} className="zip" onClick={openLink}>
                    <i className="material-icons">archive</i>
                </a>
                <a href={!hidden && API.m3uLink(path())} className="m3u" onClick={openLink}>
                    m3u
                </a>
            </div>}

            {!isFolder && <>
                <CircleProgress value={progress}/>
                <span className={'progress label'}>{progress}%</span> &nbsp;
                <a href={!hidden && API.downloadLink(path())} onClick={openLink}>{name}</a>
                &nbsp;&nbsp; <Size value={sizeBytes}/>
            </>}

            {!hidden && <AutocompleteSelect className="prioritySelect"
                                            scroll={SCROLLBAR_MODE_HIDDEN}
                                            value={priority || 'NORMAL'}
                                            onSelect={priority => API.getWs().send('SetFilePriority', {path: path(), priority, hash})}
                                            withArrow={false}
                                            withFilter={false}
                                            selectedMode={'inline'}
                                            data={['OFF', 'NORMAL', 'HIGH']}
            />}
        </div>
        <div className="resizeable children" style={{
            height: (35 * shownChildren) + 'px',
            [shownChildren === 0 && 'visibility']: 'hidden',
        }}>
            {children && Object.values(children).map(it => <TorrentFileTreeEntry hidden={shownChildren === 0 || hidden} {...it} key={it.id}
                                                                                 parentPath={path} hash={hash}
                                                                                 updateParentShownChildrenCount={updateShownChildrenCount}/>)}

        </div>
    </div>
}
