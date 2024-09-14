import React, {useState} from 'react';
import './Torrent.css'
import {useStore} from '../stores/StoreUtils'
import {state} from "../stores/TorrentsStore";
import * as DialogStore from "../stores/DialogStore";
import {formatNumberWithMaxLength, formatAbbreviation} from 'react-ui-basics/Size'
import {classNames, stopPropagation} from "react-ui-basics/Tools";
import Button from "react-ui-basics/Button";
import "react-ui-basics/Button.css";
import {FOLDER_FILTER_ALL, FOLDER_FILTER_DEFAULT, FOLDERS} from "../stores/FolderFilterStore.js";
import * as FolderFilterStore from "../stores/FolderFilterStore.js";
import TorrentFileTree from "./TorrentFileTree";
import API from "../network/API";
import AutocompleteSelect from "react-ui-basics/AutocompleteSelect.js";
import {SCROLLBAR_MODE_HIDDEN} from "react-ui-basics/Scrollable.js";


const formatSpeed = function (size) {
    return formatNumberWithMaxLength(size) + (formatAbbreviation(size)) + '/s'
};
const formatEta = function (remaining, speed) {
    var s = remaining / speed;
    if (!isFinite(s))
        return '';
    if (s < 60)
        return Math.floor(s) + 's';
    var m = s / 60;
    if (m < 60)
        return Math.floor(m) + 'm' + Math.floor(s % 60) + 's';
    var h = m / 60;
    if (h < 24)
        return Math.floor(h) + 'h' + Math.floor(m % 60) + 'm';
    var d = h / 24;
    return Math.floor(d) + 'd' + Math.floor(h % 24) + 'h';
}

export default ({hash}) => {
    const [selected, setSelected] = useState(false)
    const [showTree, setShowTree] = useState(false)
    const data = useStore(state, state => state[hash]);
    const folderFilter = useStore(FolderFilterStore.state)
    if (!data)
        return null

    if (folderFilter.folder !== FOLDER_FILTER_ALL) {
        if ((!data.folder && folderFilter.folder !== FOLDER_FILTER_DEFAULT) || (!!data.folder && data.folder !== folderFilter.folder))
            return null
    }

    const {status, name, size, d, ds, u, us, p, s, pt, st, progress, folder} = data

    const pauseTorrent = (e) => {
        stopPropagation(e)
        if (status === 'PAUSED' || status === 'STOPPED' || status === 'FINISHED')
            API.getWs().send('StartTorrent', {hash})
        else
            API.getWs().send('StopTorrent', {hash})
    }
    const clickTorrent = (e) => {
        setSelected(!selected)
        setShowTree(!showTree)
    }
    const deleteTorrent = (e) => {
        stopPropagation(e)
        DialogStore.show({
            title: 'Delete torrent?',
            description: <p> Are you sure that you want to delete
                <br/>
                <strong style={{fontSize: '18px'}}>'{name || 'name'}'</strong>
            </p>,
            buttons: [
                <Button className={'red'} onClick={() => {
                    API.getWs().send('DeleteTorrent', {hash, withData: true})
                    DialogStore.hide()
                }}>DELETE WITH DATA</Button>,
                <Button className={'red'} onClick={() => {
                    API.getWs().send('DeleteTorrent', {hash, withData: false})
                    DialogStore.hide()
                }}>DELETE</Button>,
                <Button onClick={DialogStore.hide}>Cancel</Button>,
            ],
            onCancel: DialogStore.hide,
        })
    }


    const showSettings = (e) => {
        stopPropagation(e)
        DialogStore.show({
            description: <div className="torrentSettings">
                <strong style={{fontSize: '16px'}}>{name}</strong>
                <br/>
                <br/>
                folder:
                <AutocompleteSelect
                    className="folderSelect"
                    scroll={SCROLLBAR_MODE_HIDDEN}
                    value={folder || 'default'}
                    onSelect={folder => {
                        API.getWs().send('MoveTorrent', {hash, folder})
                        DialogStore.hide()
                    }}
                    withArrow={false}
                    withFilter={false}
                    selectedMode={'inline'}
                    data={FOLDERS}
                />
            </div>,
            onCancel: DialogStore.hide,
        })
    }

    return <div className="Torrent">
        <div className={classNames('status-bar', status)}/>
        <div className={classNames('row', status.toLowerCase(), selected && 'selected')} onClick={clickTorrent}>
            <Button className="pause" round={true} flat={true} onClick={pauseTorrent}>
                <i className="material-icons">{status === 'PAUSED' || status === 'STOPPED' || status === 'FINISHED' ? 'play_arrow' : 'pause'}</i>
            </Button>
            <Button className="delete-left" round={true} flat={true} onClick={deleteTorrent}>
                <i className="material-icons">delete</i>
            </Button>
            <div>
                <span className="td name">{name}</span>
                <span className="td status">{status}</span>
                <span className="td size">{formatNumberWithMaxLength(size)}{formatAbbreviation(size)}</span>
                <span className="td d">↓{formatNumberWithMaxLength(d || 0)}{formatAbbreviation(d || 0)}</span>
                <span className="td ds">↓{formatSpeed(ds || 0)}</span>
                <span className="td eta">{progress === 100 || size - d === 0 ? '' : formatEta(size - d, ds)}</span>
                <span className="td u">↑{formatNumberWithMaxLength(u || 0)}{formatAbbreviation(u || 0)}</span>
                <span className="td us">↑{formatSpeed(us || 0)}</span>
                <span className="td peers">{p} {pt ? '(' + pt + ')' : ''}</span>
                <span className="td seeds">{s} {st ? '(' + st + ')' : ''}</span>

                <div className="mdl-progress">
                    <div className="progressbar bar bar1" style={{width: progress + '%'}}/>
                    <div className="bufferbar bar bar2"/>
                </div>
            </div>
            <Button className="settings" round={true} flat={true} onClick={showSettings}>
                <i className="material-icons">more_vert</i>
            </Button>
            <Button className="delete" round={true} flat={true} onClick={deleteTorrent}>
                <i className="material-icons">delete</i>
            </Button>
        </div>
        <TorrentFileTree hash={hash} show={showTree} folder={folder}/>
    </div>
}