'use strict';

var React = require('react');
var ReactDOM = require('react-dom');
var OutputsComponent = require('./OutputsComponent');
var $ = require('jquery');

$(".react-output-component").each(function() {
    var streamId = this.getAttribute('data-stream-id');
    var permissions = JSON.parse(this.getAttribute('data-permissions'));

    ReactDOM.render(<OutputsComponent streamId={streamId} permissions={permissions}/>, this);
});
