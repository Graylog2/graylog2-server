/** @jsx React.DOM */

'use strict';

var React = require('react');
var Card = require('./Card');
var ConfigurationBundles = require('../source-tagging/ConfigurationBundles');

var QuickStartCard = React.createClass({
    render: function () {
        var quickStartDescription = <p>New to Graylog2? Select a configuration bundle to get you started:</p>;

        return (
            <Card title="Quick Start" icon="icon-plane">
                {quickStartDescription}
                <ConfigurationBundles />
            </Card>
            );
    }
});

module.exports = QuickStartCard;
