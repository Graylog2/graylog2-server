'use strict';

var React = require('react');
var ReactDOM = require('react-dom');
var NodeThroughput = require('./NodeThroughput');

var throughput = document.getElementsByClassName('react-node-throughput');
if (throughput) {
    for (var i = 0; i < throughput.length; i++) {
        var elem = throughput[i];
        var nodeId = elem.getAttribute('data-node-id');
        ReactDOM.render(<NodeThroughput nodeId={nodeId}/>, elem);
    }
}
