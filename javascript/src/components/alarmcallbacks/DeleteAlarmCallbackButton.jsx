'use strict';

var React = require('react/addons');

var DeleteAlarmCallbackButton = React.createClass({
    getInitialState() {
        return {
            alarmCallback: this.props.alarmCallback
        };
    },
    componentWillReceiveProps(props) {
        this.setState(props);
    },
    handleClick(evt) {
        if(confirm("Really delete alarm destination?")) {
            this.state.onClick(this.state.alarmCallback);
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
