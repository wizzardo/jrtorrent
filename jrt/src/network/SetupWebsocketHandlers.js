import WS, {STATE_READY} from "./WS";
import * as DiskUsageStore from "../stores/DiskUsageStore";
import * as TorrentsStore from "../stores/TorrentsStore";
import * as TorrentListStore from "../stores/TorrentListStore";
import * as TorrentsFileTreeStore from "../stores/TorrentsFileTreeStore";
import * as TorrentsBitfieldStore from "../stores/TorrentsBitfieldStore";

export default () => {
    WS.addListener(state => {
        if (state === STATE_READY) {
            WS.send('GetList')
        }
    })

    WS.handlers.DiskUsage = (data) => DiskUsageStore.update(data);
    WS.handlers.TorrentUpdated = (data) => {
        TorrentsStore.update(data);
        data.bitfield && TorrentsBitfieldStore.update(data.hash, data.bitfield)
    };
    WS.handlers.TorrentAdded = (data) => {
        TorrentsStore.add(data);
        TorrentListStore.add(data.hash);
    };
    WS.handlers.TorrentDeleted = (data) => {
        TorrentListStore.remove(data.hash)
        TorrentsStore.remove(data);
    };
    WS.handlers.ListResponse = (data) => TorrentListStore.update(data);
    WS.handlers.FileTreeResponse = (data) => {
        TorrentsFileTreeStore.update(data);
        data.bitfield && TorrentsBitfieldStore.update(data.hash, data.bitfield)
    };
}