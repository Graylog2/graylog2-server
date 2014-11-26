'use strict';

var $ = require('jquery'); // excluded and shimed

var processSourcesData = (sources) => {
    var total = 0;
    sources.forEach((d) => total += d.message_count);
    sources.forEach((d) => {
        d.percentage = d.message_count / total * 100;
    });
    return sources;
};

var SourcesStore = {
    SOURCES_URL: '/a/sources',

    loadSources(range, callback) {
        var url = this.SOURCES_URL;
        if (typeof range !== 'undefined') {
            url += "?range="+range;
        }
        var successCallback = (data) => {
            var sources = processSourcesData(data);
            callback(sources);
        };
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            console.error("Loading of user sources failed with status: " + textStatus);
            console.error("Error", errorThrown);
        };
        $.getJSON(url, successCallback).fail(failCallback);
    }
};
module.exports = SourcesStore;
