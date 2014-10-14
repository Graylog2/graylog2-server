/** @jsx React.DOM */

'use strict';

var mergeInto = require('../../lib/util').mergeInto;
var AbstractEventSendingStore = require('../AbstractEventSendingStore');
var $ = require('jquery'); // excluded and shimed

var SourcesStore = {
    URL: '/a/sources',

    setSources: function (sources) {
        this._sources = sources;
        this._emitChange();
    },

    getSources: function () {
        return this._sources && JSON.parse(JSON.stringify(this._sources));
    },

    loadSources: function (range) {
        var url = this.URL;
        if (typeof range !== 'undefined') {
            url += "?range="+range;
        }
        var successCallback = (data) => this.setSources(data);
        var failCallback = (jqXHR, textStatus, errorThrown) => {
            console.error("Loading of user sources failed with status: " + textStatus);
            console.error("Error", errorThrown);
            alert("Could not retrieve sources from server - try reloading the page");
        };
        $.getJSON(url, successCallback).fail(failCallback);
    }
};
mergeInto(SourcesStore, AbstractEventSendingStore);

module.exports = SourcesStore;
