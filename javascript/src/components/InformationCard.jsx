/** @jsx React.DOM */

'use strict';

var React = require('React');
var Card = require('./Card');

var InformationCard = React.createClass({
    render: function () {
        return (
            <Card type="info">
                {this.props.children}
            </Card>
        );
    }
});

module.exports = InformationCard;