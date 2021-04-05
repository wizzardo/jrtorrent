import React from 'react';
import './TorrentsList.css'
import {state} from "../stores/TorrentListStore";
import Torrent from "./Torrent";
import {useStore} from "../stores/StoreUtils";

export const TorrentsList = () => {
    const {list} = useStore(state)
    // console.log('render TorrentsList', list)
    return <div className="TorrentsList">
        <div className="header">
            <span className="th status">STATUS</span>
            <span className="th size">SIZE</span>
            <span className="th d">DL</span>
            <span className="th ds">↓SPEED</span>
            <span className="th eta">ETA</span>
            <span className="th u">UL</span>
            <span className="th us">↑SPEED</span>
            <span className="th peers">PEERS</span>
            <span className="th seeds">SEEDS</span>
        </div>
        {list.map(hash=> <Torrent hash={hash} key={hash}/>)}
    </div>
}