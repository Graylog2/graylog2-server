'use strict';

var QueryInput = require('./QueryInput');
var SearchBar = require('./SearchBar');
var SearchResult = require('./SearchResult');
var React = require('react');

var queryInputContainer = document.getElementById('universalsearch-query');
if (queryInputContainer) {
  var queryInput = new QueryInput(queryInputContainer);
  queryInput.display();
}

var searchBarElem = document.getElementById('react-search-bar');
if (searchBarElem) {
  React.render(<SearchBar />, searchBarElem);
}

var searchResultElem = document.getElementById('react-search-result');
if (searchResultElem) {
  var query = searchResultElem.getAttribute('data-query');
  var searchResult = searchResultElem.getAttribute('data-search-result');
  if (searchResult) {
    searchResult = JSON.parse(searchResult);
  }
  React.render(<SearchResult query={query} result={searchResult}/>, searchResultElem);
}