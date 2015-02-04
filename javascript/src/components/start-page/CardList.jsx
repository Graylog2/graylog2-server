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
                    <p>Use our <a href="https://www.graylog.org/resources/getting-started/" target="_blank">getting started</a> guide to take your first steps with Graylog.</p>
                </InformationCard>
                <InformationCard>
                    <p>Need help with the search syntax&#63; Take a look at our <a href="https://www.graylog.org/documentation/general/queries/" target="_blank">documentation</a>.</p>
                </InformationCard>
            </div>
        );
    }
});

module.exports = CardList;
