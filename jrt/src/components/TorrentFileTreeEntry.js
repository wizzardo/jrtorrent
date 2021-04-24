import React, {useEffect, useState} from 'react';
import './TorrentFileTreeEntry.css'
import AutocompleteSelect from "react-ui-basics/AutocompleteSelect";
import "react-ui-basics/AutocompleteSelect.css";
import "react-ui-basics/FilteredList.css";
import "react-ui-basics/Scrollable.css";
import {SCROLLBAR_MODE_HIDDEN} from "react-ui-basics/Scrollable";
import {preventDefault, stopPropagation} from "react-ui-basics/Tools";
import API from "../network/API";

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

export const TorrentFileTreeEntry = (props) => {
    const {
        children,
        // chunksCompleted,
        // chunksCount,
        id,
        isFolder,
        name,
        priority,
        // sizeBytes,
        updateParentShownChildrenCount,
        hidden,
        parentPath,
        hash,
        open
    } = props;

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

            {!isFolder && <a href={!hidden && API.downloadLink(path())} onClick={openLink}>{name}</a>}

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
            height: (30 * shownChildren) + 'px',
            [shownChildren === 0 && 'visibility']: 'hidden',
        }}>
            {children && Object.values(children).map(it => <TorrentFileTreeEntry hidden={shownChildren === 0 || hidden} {...it} key={it.id}
                                                                                 parentPath={path} hash={hash}
                                                                                 updateParentShownChildrenCount={updateShownChildrenCount}/>)}

        </div>
    </div>
}
