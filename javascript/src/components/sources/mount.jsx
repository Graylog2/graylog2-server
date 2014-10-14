'use strict';

var React = require('react/addons');
var SourceOverview = require('./SourceOverview');

var sourceOverviewDiv = document.getElementById('react-sources');
if (sourceOverviewDiv) {
    React.renderComponent(<SourceOverview />, sourceOverviewDiv);
}

