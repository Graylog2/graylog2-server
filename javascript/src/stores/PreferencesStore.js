'use strict';

var mergeInto = require('../lib/util').mergeInto;
var AbstractEventSendingStore = require('./AbstractEventSendingStore');
var $ = require('jquery'); // excluded and shimed

var PreferencesStore = {
    URL: '/a/system/user/',
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
        if (!this._userName) {
            throw new Error("Need to load user preferences before you can save them");
        }
        var preferencesAsMap = preferences.reduce(function (obj, element) {
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
        }).done(function() {
            this.setPreferences(preferences);
            this.emit(this.DATA_SAVED_EVENT);
        }.bind(this)).fail(function(jqXHR, textStatus, errorThrown) {
            console.error("Saving of preferences for " + this._userName + " failed with status: " + textStatus);
            console.error("Error", errorThrown);
            alert("Could not save user preferences");
        }.bind(this));
    },

    loadUserPreferences: function (userName) {
        this._userName = userName;

        var url = this.URL + userName;
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
            var sortedArray = postProcessData(data.preferences || {});
            this.setPreferences(sortedArray);
            this.emit(this.DATA_LOADED_EVENT);
        }.bind(this);
        var failCallback = function (jqXHR, textStatus, errorThrown) {
            console.error("Loading of user preferences for " + userName + " failed with status: " + textStatus);
            console.error("Error", errorThrown);
            alert("Could not retrieve user preferences from server - try reloading the page");
        }.bind(this);
        $.getJSON(url, successCallback).fail(failCallback);
    }
};
mergeInto(PreferencesStore, AbstractEventSendingStore);

module.exports = PreferencesStore;
