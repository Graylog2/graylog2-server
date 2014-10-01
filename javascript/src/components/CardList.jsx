/** @jsx React.DOM */

'use strict';

var React = require('react');
var QuickStartCard = require('./QuickStartCard');
var InformationCard = require('./InformationCard');

var CardList = React.createClass({
    render: function () {
        return (
            <div className="card-list">
                <QuickStartCard />
                <InformationCard>
                    <p>Use our <a href="http://graylog2.org/getting-started" target="_blank">getting started</a> guide to take your first steps with Graylog2.</p>
                </InformationCard>
                <InformationCard>
                    <p>Need help with the search syntax? Take a look at our <a href="http://www.graylog2.org/resources/documentation/general/queries" target="_blank">documentation</a>.</p>
                </InformationCard>
            </div>
        );
    }
});

module.exports = CardList;
