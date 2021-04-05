const onFocusListeners = [];
const onBlurListeners = [];

const onFocus = () => onFocusListeners.forEach(it => it());
const onBlur = () => onBlurListeners.forEach(it => it());

const document = window.document || {};
let hidden, visibilityChange;
if (typeof document.hidden !== "undefined") { // Opera 12.10 and Firefox 18 and later support
    hidden = "hidden";
    visibilityChange = "visibilitychange";
} else if (typeof document.msHidden !== "undefined") {
    hidden = "msHidden";
    visibilityChange = "msvisibilitychange";
} else if (typeof document.webkitHidden !== "undefined") {
    hidden = "webkitHidden";
    visibilityChange = "webkitvisibilitychange";
}

const isHidden = hidden ? (() => document[hidden]) : (() => false);
const isActive = () => !isHidden();

if (hidden && visibilityChange)
    document.addEventListener(visibilityChange, () => isHidden() ? onBlur() : onFocus(), false);


export default {
    onFocus: (listener => onFocusListeners.push(listener)),
    onBlur: (listener => onBlurListeners.push(listener)),
    isActive,
    isHidden
};
