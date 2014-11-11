/** @jsx React.DOM */

'use strict';

var mergeInto = require('../../lib/util').mergeInto;
var AbstractEventSendingStore = require('../AbstractEventSendingStore');
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

    setSources(sources) {
        this._sources = sources;
        this._emitChange();
    },

    getSources() {
        return this._sources && JSON.parse(JSON.stringify(this._sources));
    },

    loadSources(range) {
        var url = this.SOURCES_URL;
        if (typeof range !== 'undefined') {
            url += "?range="+range;
        }
        var successCallback = (data) => this.setSources(processSourcesData(data));
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            console.error("Loading of user sources failed with status: " + textStatus);
            console.error("Error", errorThrown);
        };
        $.getJSON(url, successCallback).fail(failCallback);
    }
};
mergeInto(SourcesStore, AbstractEventSendingStore);

module.exports = SourcesStore;
