(function (exports) {
    'use strict';

    var AbstractEventSendingStore = {
        CHANGE_EVENT: 'CHANGE_EVENT',
        _emitChange: function () {
            this.emit(this.CHANGE_EVENT);
        },

        /**
         * @param {function} callback
         */
        addChangeListener: function (callback) {
            this.on(this.CHANGE_EVENT, callback);
        },

        /**
         * @param {function} callback
         */
        removeChangeListener: function (callback) {
            this.removeListener(this.CHANGE_EVENT, callback);
        }
    };
    util.mergeInto(AbstractEventSendingStore, EventEmitter.prototype);

    exports.AbstractEventSendingStore = AbstractEventSendingStore;

}((typeof exports === 'undefined') ? window : exports));