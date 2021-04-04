import React from 'react';
import {formatAbbreviation, formatNumberWithMaxLength} from 'react-ui-basics/Size';
import './DiskUsage.css'
import { useStore } from 'laco-react'
import {state} from "../stores/DiskUsageStore";

export const DiskUsage = () => {
    const {free} = useStore(state)

    return <div className="DiskUsage">
        <span className="info">Free disk space: </span>
        {/*<Size value={free || 0}/>*/}
        <span className="value">{formatNumberWithMaxLength(free || 0)}</span>
        <span className="info">{formatAbbreviation(free || 0)}</span>
    </div>
}