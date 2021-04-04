import {GET} from "./ReST";
import {getConfig} from './Config'
import WS from "./WS";
import API from "./API";
import SetupWebsocketHandlers from './SetupWebsocketHandlers'

export default () => {
    if (window.location.origin.indexOf('localhost') !== -1
        || window.location.origin.indexOf('0.0.0.0') !== -1
        || window.location.origin.indexOf('192.168') !== -1
        || window.location.origin.indexOf('127.0.0.1') !== -1) {
        GET('http://localhost:8084/jrt/info', {}, null
            , () => getConfig().base = ('//localhost:8084/jrt/')
            , () => console.error('cannot connect to local backend')
            , 1000 /*timeout*/
            , false /*sync*/
        );
    } else {
        getConfig().base = (window.location.origin + '/jrt/');
    }

    SetupWebsocketHandlers()

    API.getSelf().then(loginData => {
        API.setToken(loginData.token);
        WS.connect();
    })
}