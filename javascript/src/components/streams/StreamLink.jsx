/* global jsRoutes */

'use strict';

var React = require('react/addons');

var StreamLink = React.createClass({
    render() {
        var stream = this.props.stream;
        var url = jsRoutes.controllers.StreamSearchController.index(stream.id, "*", "relative", 300).url;
        return <a href={url}>{stream.title}</a>;
    }
});

module.exports = StreamLink;
