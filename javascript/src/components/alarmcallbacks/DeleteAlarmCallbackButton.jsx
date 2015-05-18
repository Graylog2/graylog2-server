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
            <button className="btn btn-danger" onClick={this.handleClick}>
                Delete callback
            </button>
        );
    }
});

module.exports = DeleteAlarmCallbackButton;
