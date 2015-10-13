'use strict';

var React = require('react');
var ReactDOM = require('react-dom');
var AlarmCallbackComponent = require('./AlarmCallbackComponent');
var $ = require('jquery');

$(".react-alarmcallback-component").each(function() {
    var streamId = this.getAttribute('data-stream-id');
    var permissions = JSON.parse(this.getAttribute('data-permissions'));
    var component = <AlarmCallbackComponent streamId={streamId} permissions={permissions}/>;

    ReactDOM.render(component, this);
});
