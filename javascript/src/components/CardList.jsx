/** @jsx React.DOM */

'use strict';

var React = require('react');
var QuickStartCard = require('./QuickStartCard');
var InformationCard = require('./InformationCard');

var CardList = React.createClass({
    render: function () {
        var contentInfo = <p>Need help with the search syntax? Take a look at our <a href="http://www.graylog2.org/resources/documentation/general/queries" target="_blank">documentation</a>.</p>;
        return (
            <div className="card-list">
                <QuickStartCard />
                <InformationCard>
                    {contentInfo}
                </InformationCard>
            </div>
        );
    }
});

module.exports = CardList;
