'use strict';

var React = require('react');
var Card = require('./Card');
var ConfigurationBundles = require('../source-tagging/ConfigurationBundles');

var QuickStartCard = React.createClass({
    render() {
        var quickStartDescription = <p>New to Graylog&#63; Select a configuration bundle to get you started:</p>;

        return (
            <Card title="Quick Start" icon="icon-plane">
                {quickStartDescription}
                <ConfigurationBundles />
            </Card>
            );
    }
});

module.exports = QuickStartCard;
