import {DELETE, GET, POST, POST_MULTIPART, toRequestParams} from './ReST'
import {
    Constants, getConfig,
    showLog,
    showStats
} from './Config';
import WS from "./WS";

const config = getConfig();

export const setToken = (token) => config.token = token;
export const setPlatform = (platform) => config.platform = platform;
export const setPlatformVersion = (platformVersion) => config.platformVersion = platformVersion;
export const setAppVersion = (version) => config.appVersion = version;
export const setBasePath = (path) => config.base = path;
export const getWs = () => WS;
export const setTransport = (transport) => config.transport = transport;
export const closeWs = WS.close;


const pattern = /\{(\w+)\}/g;
const makeUrl = (template) => {
    let parts = [];
    let variables = [];

    let find;
    let prevIndex = 0;
    while ((find = pattern.exec(template)) !== null) {
        variables.push(find[1]);
        parts.push(template.substring(prevIndex, find.index));
        prevIndex = find.index + find[0].length;
    }
    if (prevIndex === 0)
        return () => template;

    parts.push(template.substring(prevIndex, template.length));

    const m = parts;
    const v = variables;
    return function (id, params) {
        const length = Math.max(m.length, v.length);
        let s = '';
        for (let i = 0; i < length; i++) {
            if (m[i] !== null)
                s += m[i];
            if (v[i] === 'id' && id != null)
                s += id;
            else if (params && v[i] !== null && params[v[i]] != null)
                s += encodeURIComponent(params[v[i]]);
        }
        return s;
    };
};

const createGET = (template, commandName) => {
    let url = null;
    return (id, params, onSuccess, onError, timeout) =>
        GET(`${config.base}${(url ? url : url = makeUrl(template))(id, params)}`, params, config.token, onSuccess, onError, timeout, true);
};
const createPOST = (template, commandName) => {
    let url = null;
    return (id, params, onSuccess, onError, timeout) =>
        POST(`${config.base}${(url ? url : url = makeUrl(template))(id, params)}`, params, config.token, onSuccess, onError, timeout, true);
};
const createMultipart = (template) => {
    let url = makeUrl(template);
    return (id, params, onSuccess, onError, timeout, onProgress, provideCancel) =>
        POST_MULTIPART(`${config.base}${url(id, params)}`, params, config.token, onSuccess, onError, timeout, true, onProgress, provideCancel);
};
const createDELETE = (template) => {
    let url = makeUrl(template);
    return (id, params, onSuccess, onError, timeout) => DELETE(`${config.base}${url(id, params)}`, params, config.token, onSuccess, onError, timeout, true);
};

export default  {
    Constants,
    setToken,
    setPlatform,
    setPlatformVersion,
    setAppVersion,
    setBasePath,
    setTransport,
    showLog,
    showStats,
    closeWs,
    getWs,

    getSelf: createGET(`/users/self`),
};