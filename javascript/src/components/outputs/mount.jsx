'use strict';

var React = require('react/addons');
var OutputsComponent = require('./OutputsComponent');
var $ = require('jquery'); // excluded and shimed

$(".react-output-component").each(function() {
    var streamId = this.getAttribute('data-stream-id');
    var permissions = this.getAttribute('data-permissions');

    React.render(<OutputsComponent streamId={streamId} permissions={permissions}/>, this);
});
