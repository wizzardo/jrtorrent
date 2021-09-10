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
import * as TorrentsFileTreeStore from "../stores/TorrentsFileTreeStore";
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

export const countOpenEntries = (children) => {
    if (!children)
        return 0
    let count = 0;
    for (const key in children) {
        const child = children[key];
        count += 1
        if (child.open)
            count += countOpenEntries(child.children)
    }
    return count
}

export const TorrentFileTreeEntry = (props) => {
    const {
        parentPath,
        hash,
        path,
        forceOpen,
    } = props;
    const bitfield = useStore(TorrentsBitfieldStore.state, s => s[hash])

    const data = useStore(TorrentsFileTreeStore.state, TorrentsFileTreeStore.selectPath(hash, path))

    const {
        // chunksCompleted,
        // chunksCount,
        children,
        id,
        isFolder,
        name,
        priority,
        piecesOffset,
        piecesLength,
        sizeBytes,
        open,
    } = data || {};

    const toggleChildren = (e) => {
        if (e.processed)
            return;
        e.processed = true;
        TorrentsFileTreeStore.setOpen(hash, path, !open)
    };

    useEffect(() => !!forceOpen && TorrentsFileTreeStore.setOpen(hash, path, !!forceOpen), [!!forceOpen])

    const pathEncoded = parentPath + '/' + encodeURIComponent(name)

    const [progress, setProgress] = useState(0);
    useEffect(() => {
        if (!isFolder)
            setProgress(countProgress(bitfield, piecesOffset, piecesLength));
    }, [bitfield])

    const shownChildren = open ? countOpenEntries(children) + 0 : 0

    // console.log('TorrentFileTreeEntry', shownChildren, open, data.open, path, data)
    debugger

    return <div className="TorrentFileTreeEntry" onClick={toggleChildren}>
        <div className="info">
            {isFolder && <div className="folder">
                <i className="material-icons">{open ? 'folder_open' : 'folder'}</i>
                <span className="folderName">{name}</span>

                <a href={API.zipLink(pathEncoded)} className="zip" onClick={openLink}>
                    <i className="material-icons">archive</i>
                </a>
                <a href={API.m3uLink(pathEncoded)} className="m3u" onClick={openLink}>
                    m3u
                </a>
            </div>}

            {!isFolder && <>
                <CircleProgress value={progress}/>
                <span className={'progress label'}>{progress}%</span> &nbsp;
                <a href={API.downloadLink(pathEncoded)} onClick={openLink}>{name}</a>
                &nbsp;&nbsp; <Size value={sizeBytes}/>
            </>}

            {<AutocompleteSelect className="prioritySelect"
                                 scroll={SCROLLBAR_MODE_HIDDEN}
                                 value={priority || 'NORMAL'}
                                 onSelect={priority => API.getWs().send('SetFilePriority', {path: pathEncoded, priority, hash})}
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
            {!!open && children && Object.values(children).map(it => <TorrentFileTreeEntry
                key={it.id}
                path={[...path, it.name]}
                parentPath={pathEncoded}
                hash={hash}
                name={it.name}
            />)}

        </div>
    </div>
}
