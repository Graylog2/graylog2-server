'use strict';

declare var $: any;

import UserNotification = require("../../util/UserNotification");

interface Source {
    name: string;
    message_count: number;
    percentage: number;
}

var processSourcesData = (sources: Array<Source>): Array<Source> => {
    var total = 0;
    sources.forEach((d) => total += d.message_count);
    sources.forEach((d) => {
        d.percentage = d.message_count / total * 100;
    });
    return sources;
};

var SourcesStore = {
    SOURCES_URL: '/a/sources',

    loadSources(range: number, callback: (sources: Array<Source>) => void) {
        var url = this.SOURCES_URL;
        if (typeof range !== 'undefined') {
            url += "?range="+range;
        }
        var successCallback = (data) => {
            var sources = processSourcesData(data);
            callback(sources);
        };
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            UserNotification.error("Loading of sources data failed with status: " + errorThrown + ". Try reloading the page.",
                "Could not load sources data");
        };
        $.getJSON(url, successCallback).fail(failCallback);
    }
};

export = SourcesStore;
