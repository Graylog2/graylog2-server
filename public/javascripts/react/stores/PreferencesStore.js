(function (exports, mergeInto) {
    'use strict';

    var PreferencesStore = {
        // TODO
        URL: '/a/system/inputs',
        DATA_SAVED_EVENT: 'DATA_SAVED_EVENT',
        DATA_LOADED_EVENT: 'DATA_LOADED_EVENT',

        setPreferences: function (preferences) {
            this._preferences = preferences;
            this._emitChange();
        },

        getPreferences: function () {
            return this._preferences;
        },

        saveUserPreferences: function (preferences) {
            // optimistic update
            this.setPreferences(preferences);
            this.emit(this.DATA_SAVED_EVENT);

            // TODO: Save to server

        },

        loadUserPreferences: function (userName) {
            this._userName = userName;

            // TODO: Dummy data
            this.setPreferences({
                updateUnfocussed: true
            });
            this.emit(this.DATA_LOADED_EVENT);

//            var url = URI(this.URL).directory(userName);
//            var successCallback = function (data) {
//                this.setPreferences(data.preferences || {});
//            }.bind(this);
//            var failCallback = function (jqXHR, textStatus, errorThrown) {
//                console.error("Loading of user preferences for " + userName + " failed with status: " + textStatus);
//                console.error("Error", errorThrown);
//                alert("Could not retrieve user preferences from server - try reloading the page");
//            }.bind(this);
//            $.getJSON(url, successCallback).fail(failCallback);
        }
    };
    mergeInto(PreferencesStore, AbstractEventSendingStore);

    exports.PreferencesStore = PreferencesStore;

})((typeof exports === 'undefined') ? window : exports, util.mergeInto);
