'use strict';

var React = require('react/addons');

var DeleteAlarmCallbackButton = React.createClass({
    handleClick(evt) {
        if(window.confirm("Really delete alarm destination?")) {
            this.props.onClick(this.props.alarmCallback);
        }
    },
    render() {
        return (
            <button className="btn btn-danger btn-xs" onClick={this.handleClick}>
                <i className="fa fa-remove"></i>  Delete
            </button>
        );
    }
});

module.exports = DeleteAlarmCallbackButton;
