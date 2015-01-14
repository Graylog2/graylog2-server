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
            UserNotification.warning("Loading Grok patterns failed with status: " + errorThrown,
                "Could not load grok patterns");
        };
        // get the current list of patterns and sort it by name
        $.getJSON(this.URL, (patterns: Array<GrokPattern>) => {
            patterns.sort((pattern1, pattern2) => {
                return pattern1.name.toLowerCase().localeCompare(pattern2.name.toLowerCase());
            });
            callback(patterns);
        }).fail(failCallback);
    },
    
    savePattern(pattern: GrokPattern, callback: () => void) {
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
        }).fail((jqXHR, textStatus, errorThrown) => {
            console.log("could not update pattern " + pattern + ": " + errorThrown);
        });
    },
    
    deletePattern(pattern: GrokPattern, callback: () => void) {
        $.ajax({
            type: "DELETE",
            url: this.URL + "/" + pattern.id
        }).done(callback)
            .fail((jqXHR, textStatus, errorThrown) => {
                console.log("Unable not delete pattern " + pattern + ": " + errorThrown);
            });
    }
};

export = GrokPatternsStore;