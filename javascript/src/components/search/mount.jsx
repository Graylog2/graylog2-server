'use strict';

var QueryInput = require('./QueryInput');
var SearchBar = require('./SearchBar');
var MessageShow = require('./MessageShow');
var SearchResult = require('./SearchResult');
var React = require('react');
var Immutable = require('immutable');
var SearchStore = require('../../stores/search/SearchStore');

var queryInputContainer = document.getElementById('universalsearch-query');
if (queryInputContainer) {
    var queryInput = new QueryInput(queryInputContainer);
    queryInput.display();
}

var searchBarElem = document.getElementById('react-search-bar');
if (searchBarElem) {
    SearchStore.initializeFieldsFromHash();
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

    var searchInStreamId = searchResultElem.getAttribute('data-search-in-stream');
    SearchStore.searchInStreamId = searchInStreamId;

    React.render(<SearchResult query={query}
                               result={searchResult}
                               histogram={histogram}
                               formattedHistogram={formattedHistogram}
                               streams={Immutable.Map(streams)}
                               inputs={Immutable.Map(inputs)}
                               nodes={Immutable.Map(nodes)}
                               searchInStreamId={searchInStreamId}
        />, searchResultElem);
}