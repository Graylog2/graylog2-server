'use strict';

var React = require('react');
var ReactDOM = require('react-dom');
var CollectorList = require('./CollectorList');

var collectorList = document.getElementById('react-collector-list');
if (collectorList) {
    ReactDOM.render(<CollectorList />, collectorList);
}
