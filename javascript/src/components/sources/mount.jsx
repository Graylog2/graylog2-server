'use strict';

var React = require('react');
var SourceOverview = require('./SourceOverview');

var sourceOverviewDiv = document.getElementById('react-sources');
if (sourceOverviewDiv) {
  React.render(<SourceOverview/>, sourceOverviewDiv);
}
