'use strict';

var mergeInto = require('../../lib/util').mergeInto;
var AbstractEventSendingStore = require('../AbstractEventSendingStore');
var $ = require('jquery'); // excluded and shimed

var PreferencesStore = {
    URL: '/a/system/user/',
    DATA_SAVED_EVENT: 'DATA_SAVED_EVENT',
    DATA_LOADED_EVENT: 'DATA_LOADED_EVENT',

    setPreferences(preferences) {
        this._preferences = preferences;
        this._emitChange();
    },

    getPreferences() {
        return this._preferences && this._preferences.slice();
    },

    getUserName() {
        return this._userName;
    },

    saveUserPreferences(preferences) {
        if (!this._userName) {
            throw new Error("Need to load user preferences before you can save them");
        }
        var preferencesAsMap = preferences.reduce((obj, element) => {
            obj[element.name] = element.value;
            return obj;
        }, {});

        var url = this.URL + this._userName + "/preferences";
        $.ajax({
            type: "PUT",
            url: url,
            data: JSON.stringify(preferencesAsMap),
            dataType: 'json',
            contentType: 'application/json'
        }).done(() => {
            this.setPreferences(preferences);
            this.emit(this.DATA_SAVED_EVENT);
        }).fail((jqXHR, textStatus, errorThrown) => {
            console.error("Saving of preferences for " + this._userName + " failed with status: " + textStatus);
            console.error("Error", errorThrown);
            alert("Could not save user preferences");
        });
    },

    loadUserPreferences(userName) {
        this._userName = userName;

        var url = this.URL + userName;
        var postProcessData = (preferences) => {
            // turn map into array
            preferences = Object.keys(preferences).map((name) => {
                return {
                    name: name,
                    value: preferences[name]
                };
            });
            preferences = preferences.sort((t1, t2) => t1.name.localeCompare(t2.name));
            return preferences;
        };
        var successCallback = (data) => {
            var sortedArray = postProcessData(data.preferences || {});
            this.setPreferences(sortedArray);
            this.emit(this.DATA_LOADED_EVENT);
        };
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            console.error("Loading of user preferences for " + userName + " failed with status: " + textStatus);
            console.error("Error", errorThrown);
            alert("Could not retrieve user preferences from server - try reloading the page");
        };
        $.getJSON(url, successCallback).fail(failCallback);
    }
};
mergeInto(PreferencesStore, AbstractEventSendingStore);

module.exports = PreferencesStore;
