'use strict';

var SearchBar = require('./SearchBar');
var MessageShow = require('./MessageShow');
var SearchResult = require('./SearchResult');
var React = require('react');
var Immutable = require('immutable');
var SearchStore = require('../../stores/search/SearchStore');
var SavedSearchesStore = require('../../stores/search/SavedSearchesStore');

var searchBarElem = document.getElementById('react-search-bar');
if (searchBarElem) {
    SearchStore.initializeFieldsFromHash();
    SavedSearchesStore.updateSavedSearches();
    React.render(<SearchBar />, searchBarElem);
}

var messageDetails = document.getElementById('react-message-details');
if (messageDetails) {
    var message = messageDetails.getAttribute('data-message');
    if (message) {
        message = JSON.parse(message);
    }

    var inputs = messageDetails.getAttribute('data-inputs');
    if (inputs) {
        inputs = JSON.parse(inputs);
    }

    var nodes = messageDetails.getAttribute('data-nodes');
    if (nodes) {
        nodes = JSON.parse(nodes);
    }

    var streams = messageDetails.getAttribute('data-streams');
    if (streams) {
        streams = JSON.parse(streams);
    }

    React.render(<MessageShow message={message} inputs={Immutable.Map(inputs)} nodes={Immutable.Map(nodes)} streams={Immutable.Map(streams)} />, messageDetails);
}

var searchResultElem = document.getElementById('react-search-result');
if (searchResultElem) {
    var query = searchResultElem.getAttribute('data-query');
    var builtQuery = searchResultElem.getAttribute('data-built-query');

    var searchResult = searchResultElem.getAttribute('data-search-result');
    if (searchResult) {
        searchResult = JSON.parse(searchResult);
    }

    var histogram = searchResultElem.getAttribute('data-histogram');
    if (histogram) {
        histogram = JSON.parse(histogram);
    }

    var formattedHistogram = searchResultElem.getAttribute('data-formatted-histogram');
    if (formattedHistogram) {
        formattedHistogram = JSON.parse(formattedHistogram);
    }

    var streams = searchResultElem.getAttribute('data-streams');
    if (streams) {
        streams = JSON.parse(streams);
    }

    var inputs = searchResultElem.getAttribute('data-inputs');
    if (inputs) {
        inputs = JSON.parse(inputs);
    }

    var nodes = searchResultElem.getAttribute('data-nodes');
    if (nodes) {
        nodes = JSON.parse(nodes);
    }

    var searchInStream = searchResultElem.getAttribute('data-search-in-stream');
    if (searchInStream) {
        searchInStream = JSON.parse(searchInStream);
        SearchStore.searchInStream = searchInStream;
    }

    var permissions = JSON.parse(searchResultElem.getAttribute('data-permissions'));

    React.render(<SearchResult query={query}
                               builtQuery={builtQuery}
                               result={searchResult}
                               histogram={histogram}
                               formattedHistogram={formattedHistogram}
                               streams={Immutable.Map(streams)}
                               inputs={Immutable.Map(inputs)}
                               nodes={Immutable.Map(nodes)}
                               searchInStream={searchInStream}
                               permissions={permissions}
        />, searchResultElem);
}