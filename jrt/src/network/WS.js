import {getWsUrl, showLog, showStats, getConfig} from './Config'
import WindowListener from './WindowListener'
import {orNoop, clearInterval, setTimeout, setInterval} from "react-ui-basics/Tools";

const now = (window.performance && window.performance.now && (() => window.performance.now())) || (() => new Date().getTime());
let pingTimer;
const handlers = {};
const requests = {};
const wsEvents = {
    onOpen: () => {
        clearInterval(pingTimer);
        pingTimer = setInterval(() => {
            if (state === STATE_READY)
                send('Ping', {})
        }, 10000)
    },
    onClose: () => {
        clearInterval(pingTimer);
    }
};

export const STATE_CONNECTING = 'STATE_CONNECTING';
export const STATE_READY = 'STATE_READY';
export const STATE_NOT_CONNECTED = 'STATE_NOT_CONNECTED';

let state = STATE_NOT_CONNECTED;
let lastConnectTry = null;
let retryInterval = 1000;
let nextRetry = null;
var scheduledRetry;
let connectionCounter = 0;

let ws = null;
let stats = {};
const resetStats = () => {
    stats = {
        requestsCount: 0,
        totalTimeMs: 0,
        minTimeMs: 0,
        maxTimeMs: 0,
        avgTimeMs: 0,
        started: 0,
        ended: 0,
        realTimeMs: 0,
    }
};
resetStats();


const listeners = [];
const addListener = (listener) => {
    listeners.push(listener)
};
const removeListener = (listener) => {
    let i = listeners.indexOf(listener);
    if (i !== -1)
        listeners.splice(i, 1)
};
const processListeners = (state) => {
    setTimeout(() => listeners.forEach(listener => listener(state)), 0)
};

const eventListeners = {};
const addCommandListener = (command, listener) => {
    if (!eventListeners[command])
        eventListeners[command] = [];

    eventListeners[command].push(listener)
};
const removeCommandListener = (command, listener) => {
    let listeners = eventListeners[command];
    if (!listeners) return;

    let i = listeners.indexOf(listener);
    if (i !== -1)
        listeners.splice(i, 1)
};
const processCommandListeners = (command, data) => {
    let listeners = eventListeners[command];
    if (!listeners) return;

    listeners.forEach(l => {
        try {
            l(data);
        } catch (e) {
            log.error('processCommandListeners', command, data, e)
        }
    })
};

let onInvalidCredentialsListener = null;
const onInvalidCredentials = (listener) => listener ? onInvalidCredentialsListener = listener : onInvalidCredentialsListener;

const getState = () => state;
const getNextRetry = () => nextRetry;

const updateStats = (request, response) => {
    const showlog = showLog();
    const showstats = showStats();
    if (!showlog && !showstats)
        return;

    let time = now();
    let duration = time - request.now;
    showlog && log(`${request.command}.${request.requestId} took ${duration} ms, request:`, request.data, 'response:', response);

    send('Measurement', {command: request.command, duration});

    stats.requestsCount++;
    stats.totalTimeMs += duration;
    stats.ended = time;
    stats.realTimeMs = stats.ended - stats.started;
    if (stats.minTimeMs === 0 || stats.minTimeMs > duration)
        stats.minTimeMs = duration;
    if (stats.maxTimeMs === 0 || stats.maxTimeMs < duration)
        stats.maxTimeMs = duration;
    stats.avgTimeMs = stats.totalTimeMs / stats.requestsCount;
    let left = Object.keys(requests).length;
    if (left === 0) {
        showstats && log('ws requests stats:', stats);
        resetStats();
    } else {
        // log('requests left:', left);
        // if (left < 10)
        //     log('requests left:', requests);
    }
};

const log = console.log.bind(console);

const send = (name, data) => {
    if (!name)
        throw Error("command name is mandatory");

    showLog() && log('send', name, data);

    if (state === STATE_READY) {
        try {
            ws.send(name + JSON.stringify(data || {}));
        } catch (e) {
            log(e);
            state = STATE_NOT_CONNECTED;
            connect()
        }
    } else {
        if (state === STATE_NOT_CONNECTED)
            connect()
    }
};

const processRequest = (requestId, cb, data) => {
    let request = requests[requestId];
    if (!request) {
        log(`request ${requestId} not found`);
        return
    }

    if (request.timeoutExecuted) return;
    request.timeout && clearTimeout(request.timeout);

    cb(data);

    delete requests[requestId];
    // console.trace(`remove request ${requestId}`, request, cb, data);
};

const resendQueue = () => {
    Object.keys(requests).forEach(requestId => {
        let request = requests[requestId];
        send(request.command, request.data)
    })
};

function timestamp(date) {
    return (date || new Date()).toISOString();
}

function scheduleRetry() {
    nextRetry = null;
    clearTimeout(scheduledRetry)
    if (lastConnectTry && now() - lastConnectTry < retryInterval) {
        nextRetry = new Date(now() + retryInterval);
        showLog() && log("next retry in " + (retryInterval / 1000) + "s at " + timestamp(nextRetry));

        scheduledRetry = setTimeout(connect, retryInterval);
        if (retryInterval < 5000)
            retryInterval += 1000;
    }
    if (!nextRetry)
        connect();
}

const connect = () => {
    if (state === STATE_CONNECTING || state === STATE_READY)
        return;

    if (!WebSocket)
        return;

    lastConnectTry = now();

    if (!getConfig().token) {
        scheduleRetry();
        return;
    }

    state = STATE_CONNECTING;
    processListeners(state);

    const connectionId = ++connectionCounter;
    nextRetry = null;

    const url = getWsUrl();
    showLog() && log("Connection." + connectionId + " start opening at " + timestamp(), url);

    if (ws !== null) {
        ws.onclose = null;
        ws.close()
    }

    try {
        ws = new WebSocket(url);
    } catch (e) {
        log('Unexpected error while connecting', e);
        state = STATE_NOT_CONNECTED;
        processListeners(state);
        return
    }

    ws.onopen = function () {
        showLog() && log("Connection." + connectionId + " opened at " + timestamp(), url);
        orNoop(wsEvents.onOpen)();
    };
    ws.onmessage = function (e) {
        // log(e.data);
        if (e.error) {
            log(e.error.exceptionClass + ": " + e.error.message);
            log(e.error.stacktrace);
            log(e.error.case);
        }
        let commandNameLength = e.data.indexOf('{');
        if (commandNameLength === 0) {
            let response = JSON.parse(e.data);
            let requestId = response.requestId;
            let request;
            if (requestId && (request = requests[requestId])) {
                updateStats(request, response);

                setTimeout(() => {
                    if (response.status >= 200 && response.status < 300)
                        try {
                            request.onSuccess(response.data || {});
                        } catch (e) {
                            request.onError(e);
                        }
                    else
                        request.onError(response.data || {});
                }, 0);
            }else if (response.command && handlers[response.command]) {
                setTimeout(() => {
                    handlers[response.command](response)
                }, 0)
            } else {
                log("unexpected message", response);
            }
        } else {
            let command = e.data.substring(0, commandNameLength);
            if (command === 'AppInfo') {
                state = STATE_READY;
                // send('VerifyConnection', {token: getConfig().token})

                if (WindowListener.isActive())
                    resendQueue();

                retryInterval = 1000;
                clearTimeout(noPingReconnectTimeout);
                processListeners(state);

                showLog() && log("Connection ready at " + timestamp());
            }

            let data = JSON.parse(e.data.substring(commandNameLength));
            let handler = handlers[command];
            if (handler) {
                setTimeout(() => {
                    handler(data);
                }, 0)
            } else {
                log("unknown command: ", command, data);
            }
            setTimeout(() => {
                processCommandListeners(command, data)
            }, 0);
        }
    };
    ws.onclose = function (event) {
        showLog() && log("Connection." + connectionId + " closed at " + timestamp(), event);
        state = STATE_NOT_CONNECTED;

        processListeners(state);
        orNoop(wsEvents.onClose)();
        scheduleRetry();
    };
};

const close = () => {
    clearTimeout(scheduledRetry)
    state = STATE_NOT_CONNECTED;
    processListeners(state);
    if (ws) {
        ws.onclose = null;
        try {
            ws.close()
        } catch (ignore) {
        }
        orNoop(wsEvents.onClose)();
    }
};

let noPingReconnectTimeout;
handlers.Ping = () => {
    clearTimeout(noPingReconnectTimeout);
    noPingReconnectTimeout = setTimeout(() => {
        if (state === STATE_READY) {
            close();
            connect();
        }
    }, 15000);
};
handlers.Reconnect = () => {
    close();
    setTimeout(connect, 500);
};

handlers.InvalidCredentials = () => {
    close();
    orNoop(onInvalidCredentialsListener)()
};

let counter = 0;

const createRequest = (command, data, onSuccess, onError, timeout) => {
    if (!command)
        throw Error("command name is mandatory");

    const requestId = ++counter;
    data.requestId = requestId;
    const time = now();
    const promise = new Promise((resolve, reject) => {
        const success = data => processRequest(requestId, (onSuccess || resolve), data);
        const error = e => processRequest(requestId, (onError || reject), e);
        requests[requestId] = {
            onSuccess: success,
            onError: error,
            command,
            data,
            now: time,
            requestId,
            timeout: timeout && setTimeout(() => {
                requests[requestId].timeoutExecuted = true;
                (onError || reject)()
            }, timeout)
        };
        send(command, data);
    });

    if (stats.started === 0)
        stats.started = time;
    return promise;
};

export class commands {
    static Log = 'Log';
};

WindowListener.onFocus(() => setTimeout(resendQueue, 500)); // delayed resendQueue to give browser time to handle all the messages from server those were on hold because of cpu throttling in inactive tab

export default {
    connect,
    handlers,
    send,
    wsEvents,
    commands,
    createRequest,
    close,
    addListener,
    removeListener,
    addCommandListener,
    removeCommandListener,
    onInvalidCredentials,
    getState,
    getNextRetry,
    getStats: () => stats,
    requests
}
