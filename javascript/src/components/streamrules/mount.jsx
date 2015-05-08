'use strict';

var React = require('react/addons');
var StreamRulesComponent = require('./StreamRulesComponent');
var StreamRulesEditor = require('./StreamRulesEditor');
var $ = require('jquery'); // excluded and shimed

$(".react-streamrules-component").each(function() {
    var permissions = JSON.parse(this.getAttribute('data-permissions'));
    var streamId = this.getAttribute('data-stream-id');
    var component = <StreamRulesComponent permissions={permissions} streamId={streamId}/>;

    React.render(component, this);
});

$(".react-streamrules-editor").each(function() {
    var permissions = JSON.parse(this.getAttribute('data-permissions'));
    var streamId = this.getAttribute('data-stream-id');
    var component = <StreamRulesEditor permissions={permissions} streamId={streamId}/>;

    React.render(component, this);
});
