declare var $: any;

import UserNotification = require("../../UserNotification");

interface Preference {
    name: string;
    value: any
}

interface PreferencesMap {
    [index: string]: any;
}

interface Data {
    preferences: PreferencesMap
}

var PreferencesStore = {
    URL: '/a/system/user/',
    convertPreferenceMapToArray(preferencesAsMap: PreferencesMap): Array<Preference> {
        preferencesAsMap = preferencesAsMap || {};
        var preferences = Object.keys(preferencesAsMap).map((name) => {
            return {
                name: name,
                value: preferencesAsMap[name]
            };
        });
        preferences = preferences.sort((t1, t2) => t1.name.localeCompare(t2.name));
        return preferences;
    },
    convertPreferenceArrayToMap: function (preferences: Array<Preference>): PreferencesMap {
        var preferencesAsMap: PreferencesMap = {};
        preferences.forEach((element) => {
            preferencesAsMap[element.name] = element.value;
        });
        return preferencesAsMap;
    },
    saveUserPreferences(preferences: Array<Preference>, callback: (preferences: Array<any>) => void): void {
        if (!this._userName) {
            throw new Error("Need to load user preferences before you can save them");
        }
        var preferencesAsMap = this.convertPreferenceArrayToMap(preferences);
        var url = this.URL + this._userName + "/preferences";
        $.ajax({
            type: "PUT",
            url: url,
            data: JSON.stringify(preferencesAsMap),
            dataType: 'json',
            contentType: 'application/json'
        }).done(() => {
            UserNotification.success("User preferences successfully saved");
            callback(preferences);
        }).fail((jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Could not save user preferences",
                "Saving of preferences for " + this._userName + " failed with status: " + errorThrown);
        });
    },
    loadUserPreferences(userName: string, callback: (preferences: Array<any>) => void): void {
        this._userName = userName;

        var url = this.URL + userName;
        var successCallback = (data: Data) => {
            var sortedArray = this.convertPreferenceMapToArray(data.preferences);
            callback(sortedArray);
        };
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Could not retrieve user preferences from server - try reloading the page",
                "Loading of user preferences for " + userName + " failed with status: " + errorThrown);
        };
        $.getJSON(url, successCallback).fail(failCallback);
    }
};

export = PreferencesStore;