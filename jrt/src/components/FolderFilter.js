import React from 'react';
import './FolderFilter.css'
import {FOLDER_FILTER_ALL, FOLDERS, state, update} from "../stores/FolderFilterStore.js";
import {useStore} from "../stores/StoreUtils";
import AutocompleteSelect from "react-ui-basics/AutocompleteSelect.js";
import {SCROLLBAR_MODE_HIDDEN} from "react-ui-basics/Scrollable.js";

export const FolderFilter = () => {
    const {folder} = useStore(state)

    return <div className="FolderFilter">
        folder filter:
        <AutocompleteSelect
            className="folderSelect"
            scroll={SCROLLBAR_MODE_HIDDEN}
            value={folder}
            onSelect={folder => update(folder)}
            withArrow={false}
            withFilter={false}
            selectedMode={'inline'}
            data={[FOLDER_FILTER_ALL, ...FOLDERS]}
        />
    </div>
}