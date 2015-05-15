'use strict';

var React = require('react/addons');
var CollectorList = require('./CollectorList');

var collectorList = document.getElementById('react-collector-list');
if (collectorList) {
    React.render(<CollectorList />, collectorList);
}
