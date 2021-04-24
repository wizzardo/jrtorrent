import React, {useState} from 'react';
import './AddButton.css'
import {useStore} from '../stores/StoreUtils'
import * as DialogStore from "../stores/DialogStore";
import Button from "react-ui-basics/Button";
import "react-ui-basics/TextField.css";
import TextField from "react-ui-basics/TextField";
import "react-ui-basics/FloatingActionButton.css";
import API from "../network/API";
import FloatingActionButton from "react-ui-basics/FloatingActionButton";
import DropFileInput from "react-ui-basics/DropFileInput";
import Checkbox from "react-ui-basics/Checkbox";
import "react-ui-basics/DropFileInput.css";
import "react-ui-basics/Dropzone.css";
import "react-ui-basics/Checkbox.css";
import {preventDefault, stopPropagation} from 'react-ui-basics/Tools'

const UploadForm = () => {

    const [selectedFile, setSelectedFile] = useState()
    const [url, setUrl] = useState('')
    const [autostart, setAutostart] = useState(true)


    const submit = () => {
        if(!selectedFile && !url)
            return false;

        API.addTorrent(null, {url, autostart: autostart ? 'on' : 'off', file: selectedFile})
    }

    return <form className={'UploadForm'} action="#" name="form">
        <TextField name="url" label={'link to torrent file or magnet link'} value={url} onChange={(e) => setUrl(e.target.value)}/>
        <br/>

        {!selectedFile && <DropFileInput
            droppable={true}
            multiple={false}
            accept={".torrent"}
            label={'Attach File'}
            onDrop={files => setSelectedFile(files[0])}/>
        }

        {selectedFile && <div className={"row"}>
            {selectedFile.name}
            <Button flat={true} round={true} className="delete" onClick={(e) => {
                preventDefault(e)
                stopPropagation(e)
                setSelectedFile(null);
            }}>
                <i className="material-icons">delete</i>
            </Button>
        </div>}

        <br/>

        <div className="row">
            <Checkbox name={'autostart'} label={'Autostart'} value={autostart} onChange={e => setAutostart(e.target.checked)}/>

            <Button className={'blue'} type="button" disabled={!selectedFile && !url} onClick={() => {
                submit()
                DialogStore.hide()
            }}>DOWNLOAD</Button>
        </div>
    </form>
}

export default () => {
    var {show} = useStore(DialogStore.state);


    const showForm = (e) => {
        e.stopPropagation()
        DialogStore.show({
            title: 'Add new torrent',
            description: <UploadForm/>,
            onCancel: DialogStore.hide,
        })
    }

    return <FloatingActionButton className={'AddButton'} onClick={showForm} hidden={show}/>
}