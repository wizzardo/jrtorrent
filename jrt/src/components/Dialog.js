import React, {useEffect, useState} from 'react';
import {state} from "../stores/DialogStore";
import 'react-ui-basics/Dialog.css'
import './Dialog.css'
import 'react-ui-basics/Modal.css'
import Modal from 'react-ui-basics/Modal'
import {classNames} from "react-ui-basics/Tools";
import Button from "react-ui-basics/Button";
import {useStore} from "../stores/StoreUtils";

export default () => {
    const {show, accept, cancel, onAccept, onCancel, title, description, buttons} = useStore(state)

    const [open, setOpen] = useState();
    const [close, setClose] = useState();

    useEffect(() => {
        if (show) {
            open && open()
        } else {
            close && close()
        }
    }, [show])

    // console.log('Dialog.render', show)

    return <Modal
        className={classNames('DialogModal')}
        open={open => setOpen(() => open)}
        close={close => setClose(() => close)}
        onClose={onCancel}
    >
        <div className="Dialog">
            {title && <div className="title">{title}</div>}
            {description && <div className="description">{description}</div>}
            <div className="row right">
                {cancel && <Button flat={true} onClick={onCancel}>{cancel}</Button>}
                {accept && <Button flat={true} onClick={onAccept}>{accept}</Button>}
                {buttons}
            </div>
        </div>
    </Modal>;
}