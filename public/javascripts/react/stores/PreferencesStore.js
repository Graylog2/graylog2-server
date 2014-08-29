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
            return this._preferences && this._preferences.slice();
        },

        getUserName: function () {
            return this._userName;
        },

        saveUserPreferences: function (preferences) {
            // optimistic update
            this.setPreferences(preferences);
            this.emit(this.DATA_SAVED_EVENT);

            // TODO: Save to server

        },

        loadUserPreferences: function (userName) {
            this._userName = userName;

            var url = URI(this.URL).directory(userName);
            var postProcessData = function (preferences) {
                // turn map into array
                preferences = Object.keys(preferences).map(function (name) {
                    return {
                        name: name,
                        value: preferences[name]
                    };
                });
                preferences = preferences.sort(function (t1, t2) {
                    return t1.name.localeCompare(t2.name);
                });
                return preferences;
            };
            var successCallback = function (data) {
                var sortedArray = postProcessData(data.preferences || {})
                this.setPreferences(sortedArray);
                this.emit(this.DATA_LOADED_EVENT);
            }.bind(this);
            var failCallback = function (jqXHR, textStatus, errorThrown) {
                console.error("Loading of user preferences for " + userName + " failed with status: " + textStatus);
                console.error("Error", errorThrown);
                alert("Could not retrieve user preferences from server - try reloading the page");
            }.bind(this);
//            $.getJSON(url, successCallback).fail(failCallback);
            // TODO: Dummy data
            if (!this.getPreferences()) {

                successCallback({
                    preferences: {
                        updateUnfocussed: true,
                        autoExecuteQuery: false,
                        defaultSearch: "S1",
                        moar: "10",
                        moar2: "20",
                        moar3: "30",
                        moar4: "30",
                        moar5: "30",
                        moar6: "30",
                        moar7: "30",
                        moar8: "30",
                        moar9: "30"
                    }
                });
            } else {
                this.emit(this.DATA_LOADED_EVENT);
            }
        }
    };
    mergeInto(PreferencesStore, AbstractEventSendingStore);

    exports.PreferencesStore = PreferencesStore;

})((typeof exports === 'undefined') ? window : exports, util.mergeInto);
