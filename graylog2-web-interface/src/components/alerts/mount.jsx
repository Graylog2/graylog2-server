'use strict';

var React = require('react');
var ReactDOM = require('react-dom');
var AlertsComponent = require('./AlertsComponent');
var $ = require('jquery'); // excluded and shimed

$(".react-alerts-component").each(function() {
    var streamId = this.getAttribute('data-stream-id');
    var permissions = JSON.parse(this.getAttribute('data-permissions'));
    var component = <AlertsComponent streamId={streamId} permissions={permissions}/>;

    ReactDOM.render(component, this);
});
