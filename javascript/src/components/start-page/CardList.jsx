'use strict';

var React = require('react');
var QuickStartCard = require('./QuickStartCard');
var InformationCard = require('./InformationCard');

var CardList = React.createClass({
    render() {
        return (
            <div className="card-list">
                <QuickStartCard />
                <InformationCard>
                    <p>Use our <a href="http://graylog2.org/getting-started" target="_blank">getting started</a> guide to take your first steps with Graylog.</p>
                </InformationCard>
                <InformationCard>
                    <p>Need help with the search syntax&#63; Take a look at our <a href="http://www.graylog2.org/resources/documentation/general/queries" target="_blank">documentation</a>.</p>
                </InformationCard>
            </div>
        );
    }
});

module.exports = CardList;
