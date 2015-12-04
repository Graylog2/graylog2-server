'use strict';

var React = require('react');
var ReactDOM = require('react-dom');
var SourceOverview = require('./SourceOverview');

var sourceOverviewDiv = document.getElementById('react-sources');
if (sourceOverviewDiv) {
  ReactDOM.render(<SourceOverview/>, sourceOverviewDiv);
}
