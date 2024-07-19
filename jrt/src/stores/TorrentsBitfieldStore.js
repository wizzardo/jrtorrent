import {Store} from 'laco'
import * as BitSet from "typedfastbitset";

export const state = new Store({})

const concatTypedArrays = (a, b) => { // a, b TypedArray of same type
    var c = new (a.constructor)(a.length + b.length);
    c.set(a, 0);
    c.set(b, a.length);
    return c;
};

export const update = (hash, bitfieldBase64) => {
    let uint8Array = Uint8Array.from(atob(bitfieldBase64), c => c.charCodeAt(0));
    const alignedLength = Math.ceil(uint8Array.length / 4) * 4;
    if (uint8Array.length !== alignedLength) {
        uint8Array = concatTypedArrays(uint8Array, new Uint8Array(alignedLength - uint8Array.length))
    }
    const uint32Array = new Uint32Array(uint8Array.buffer);
    const bitset = BitSet.TypedFastBitSet.fromWords(uint32Array)
    state.set(prev => ({...prev, [hash]: bitset}));
}

export const remove = (hash) => state.set(prev => {
    const next = {...prev};
    delete next[hash];
    return next;
})
