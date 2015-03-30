'use strict';

declare var $: any;

import UserNotification = require("../../util/UserNotification");
import URLUtils = require("../../util/URLUtils");

interface GrokPattern {
    id: string;
    name: string;
    pattern: string;
}

var GrokPatternsStore = {
    URL: URLUtils.appPrefixed('/a/system/grokpatterns'),

    loadPatterns(callback: (patterns: Array<GrokPattern>) => void) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Loading Grok patterns failed with status: " + errorThrown,
                "Could not load Grok patterns");
        };
        // get the current list of patterns and sort it by name
        $.getJSON(this.URL, (patterns: Array<GrokPattern>) => {
            patterns.sort((pattern1: GrokPattern, pattern2: GrokPattern) => {
                return pattern1.name.toLowerCase().localeCompare(pattern2.name.toLowerCase());
            });
            callback(patterns);
        }).fail(failCallback);
    },

    savePattern(pattern: GrokPattern, callback: () => void) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Saving Grok pattern \"" + pattern.name + "\" failed with status: " + errorThrown,
                "Could not save Grok pattern");
        };

        var url;
        if (pattern.id === "") {
            url = this.URL + '/create';
        } else {
            url = this.URL + '/update';
        }
        $.ajax({
            type: "POST",
            contentType: "application/json",
            url: url,
            data: JSON.stringify(pattern)
        }).done(() => {
            callback();
            var action = pattern.id === "" ? "created" : "updated";
            var message = "Grok pattern \"" + pattern.name + "\" successfully " + action;
            UserNotification.success(message);
        }).fail(failCallback);
    },

    deletePattern(pattern: GrokPattern, callback: () => void) {
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Deleting Grok pattern \"" + pattern.name + "\" failed with status: " + errorThrown,
                "Could not delete Grok pattern");
        };
        $.ajax({
            type: "DELETE",
            url: this.URL + "/" + pattern.id
        }).done(() => {
            callback();
            UserNotification.success("Grok pattern \"" + pattern.name + "\" successfully deleted");
        }).fail(failCallback);
    }
};

export = GrokPatternsStore;