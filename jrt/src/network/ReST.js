import {orNoop} from "react-ui-basics/Tools";

/*eslint no-use-before-define: "off"*/
var global = global || window;
global && (global.FormData = global.originalFormData ? global.originalFormData : global.FormData);

const CONTENT_TYPE_APPLICATION_JSON = 'application/json';

const toKeyValue = (key, value) => encodeURIComponent(key) + '=' + encodeURIComponent(value !== null && typeof value === 'object' ? JSON.stringify(value) : value);

export const toRequestParams = (params) => (params && Object.keys(params).map(key => {
    const value = params[key];
    if (Array.isArray(value))
        return value.map(it => toKeyValue(key, it)).join('&')
    else
        return toKeyValue(key, value);
}).join('&')) || '';

const FETCH = (method, url, params, token, onSuccess, onError, multipart, isAsync, timeout, onTimeout, onProgress, provideCancel) => {
    if (method === 'GET' || method === 'DELETE') {
        params = params || {};
        // params.rnd = Math.random(); // temporary fix to force no-cache policy
        const serializedData = toRequestParams(params);
        if (serializedData)
            url = url + "?" + serializedData;
    }

    let headers = {
        'Accept': CONTENT_TYPE_APPLICATION_JSON
    };
    if (token)
        headers.token = token;

    let config = {
        credentials: 'include',
        method: method,
        headers: headers
    };

    if (method === 'POST' || method === 'PUT') {
        if (multipart) {
            let formData;
            if (params instanceof FormData) {
                formData = params;
            } else {
                formData = new FormData();
                Object.keys(params).forEach(name => {
                    let value = params[name];
                    if (Array.isArray(value)) {
                        value.forEach(it => formData.append(name, it))
                    } else if (value != null)
                        formData.append(name, value);
                });
            }
            config.body = formData;
        } else {
            headers['Content-Type'] = CONTENT_TYPE_APPLICATION_JSON;
            config.body = JSON.stringify(params || {});
        }
    }

    let makeRequest = (success, error) => ajax(url, {
        method,
        isAsync,
        headers,
        credentials: config.credentials,
        data: config.body,
        onSuccess: success,
        onError: error,
        timeout,
        onTimeout,
        onProgress,
        provideCancel
    });

    if (!window.Promise)
        return makeRequest(onSuccess, onError);

    return new Promise((resolve, reject) => {
        const success = data => (onSuccess || resolve)(data);
        const error = (e, status) => (onError || reject)(e, status);
        makeRequest(success, error);
    });
};

export const ajax = function (url, config) {
    config = config || {};
    const request = new XMLHttpRequest();

    const method = config.method || 'GET';
    const data = config.data;
    if (method === 'GET' && data) {
        let params = toRequestParams(data);
        if (params)
            url += "?" + params;
    }

    if (config.onprogress)
        request.upload.onprogress = e => {
            if (e.lengthComputable)
                config.onprogress(e);
        };

    if (config.credentials === 'include')
        request.withCredentials = true;

    const async = !!(config.isAsync === void 0 ? true : config.isAsync);
    request.open(method, url, async);

    Object.keys(config.headers || {}).forEach(key =>
        request.setRequestHeader(key, config.headers[key])
    );
    const onError = orNoop(config.onError);

    if (config.timeout && async) {
        request.timeout = config.timeout;
        request.ontimeout = (e) => (config.onTimeout || onError)(e);
    }

    request.onprogress = config.onProgress;
    if (request.upload)
        request.upload.onprogress = config.onProgress;

    request.onload = () => {
        const responseText = request.responseText;
        const status = request.status;
        if (status >= 200 && status < 400) {
            try {
                orNoop(config.onSuccess)(JSON.parse(responseText || "{}"));
            } catch (e) {
                console.log(`Unexpected exception while processing response for ${method} ${url}, status: ${status}, response: '${responseText}', exception:`, e)
                onError(responseText, status);
            }
        } else {
            console.log(`Not ok response for ${method} ${url}, status: ${status}, response: '${responseText}'`);
            onError(responseText, status);
        }
    };

    request.onerror = onError;

    try {
        if (method === 'POST') {
            if (typeof data === 'string' || data instanceof FormData)
                request.send(data);
            else
                request.send(toRequestParams(data));
        } else
            request.send();
    } catch (e) {
        onError(e);
    }

    if (config.provideCancel) {
        if ((typeof config.provideCancel) === 'function') {
            config.provideCancel(() => request.abort());
        } else {
            throw new Error('Expecting function in "provideCancel" field, but was ' + JSON.stringify(config.provideCancel))
        }
    }
    return request
};

export const GET = (url, params, token, onSuccess, onError, timeout, isAsync) => FETCH('GET', url, params, token, onSuccess, onError, false, isAsync, timeout);

export const POST = (url, data, token, onSuccess, onError, timeout, isAsync) => FETCH('POST', url, data, token, onSuccess, onError, false, isAsync, timeout);

export const POST_MULTIPART = (url, data, token, onSuccess, onError, timeout, isAsync, onProgress, provideCancel) => FETCH('POST', url, data, token, onSuccess, onError, true, isAsync, timeout, null, onProgress, provideCancel);

export const DELETE = (url, params, token, onSuccess, onError, timeout, isAsync) => FETCH('DELETE', url, params, token, onSuccess, onError, false, isAsync, timeout);