'use strict';

var EventEmitter = require('events').EventEmitter;
var mergeInto = require('../lib/util').mergeInto;

var AbstractEventSendingStore = {
    CHANGE_EVENT: 'CHANGE_EVENT',
    _emitChange() {
        this.emit(this.CHANGE_EVENT);
    },

    /**
     * @param {function} callback
     */
    addChangeListener(callback) {
        this.on(this.CHANGE_EVENT, callback);
    },

    /**
     * @param {function} callback
     */
    removeChangeListener(callback) {
        this.removeListener(this.CHANGE_EVENT, callback);
    }
};
mergeInto(AbstractEventSendingStore, EventEmitter.prototype);

module.exports = AbstractEventSendingStore;
