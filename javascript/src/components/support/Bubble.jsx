'use strict';

var React = require('react/addons');

var Bubble = React.createClass({
    render() {
        return (
            <a href={"https://www.graylog.org/documentation/" + this.props.link} target="_blank">
                <i className="fa fa-lightbulb"></i>
            </a>
        );
    }
});

module.exports = Bubble;
