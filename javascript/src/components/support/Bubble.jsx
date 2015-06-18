'use strict';

var React = require('react');

var Bubble = React.createClass({
    render() {
        return (
            <a href={"https://www.graylog.org/documentation/" + this.props.link} target="_blank" title={this.props.title}>
                <i className="fa fa-lightbulb-o"></i>
            </a>
        );
    }
});

module.exports = Bubble;
