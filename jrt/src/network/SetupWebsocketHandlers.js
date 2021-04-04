import WS from "./WS";
import * as DiskUsageStore from "../stores/DiskUsageStore";

export default () => {
    WS.handlers.DiskUsage = (data) => DiskUsageStore.update(data)
    WS.handlers.TorrentUpdated = (data) => {} // do nothing for now
}