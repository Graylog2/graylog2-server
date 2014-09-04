/** @jsx React.DOM */

'use strict';

var React = require('React');
var Card = require('./Card');

var CardList = React.createClass({
    render: function () {
        var contentInfo = <p>Need help with the search syntax? Take a look at our <a href="http://www.graylog2.org/resources/documentation/general/queries" target="_blank">documentation</a>.</p>;
        return (
            <div className="card-list">
                <Card title="Quick Start">
                    {"I'm a card!!!"}
                </Card>
                <Card type="info">
                    {contentInfo}
                </Card>
            </div>
        );
    }
});

module.exports = CardList;