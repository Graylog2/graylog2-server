/** @jsx React.DOM */

'use strict';

var React = require('React');

var Card = React.createClass({
    render: function () {
        var icon = null;

        if (this.props.type == "info") {
            icon = <i className="icon icon-lightbulb pull-left"></i>;
        }

        return (
            <div className="card">
                {icon}
                {this.props.children}
            </div>
            );
    }
});

module.exports = Card;