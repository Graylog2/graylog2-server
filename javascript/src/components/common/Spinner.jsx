'use strict';

var React = require('react/addons');

var Spinner = React.createClass({
    getInitialState() {
        return {};
    },
    getDefaultProps() {
        return { text: "Loading..." };
    },
    render() {
        return (<div><i className="fa fa-spin fa-spinner"/> {this.props.text}</div>);
    }
});

module.exports = Spinner;
