'use strict';

var React = require('react/addons');

var DeleteAlarmCallbackButton = React.createClass({
    getInitialState() {
        return {};
    },
    render() {
        var style = {
            position: 'relative',
            top: '-1px'
        };
        return (
            <form action="@routes.AlarmCallbacksController.delete(stream.getId, callback.getId)" method="POST" style={style}>
                <button type="submit" className="btn btn-danger btn-xs" data-confirm="Really delete alarm destination?">Delete</button>
            </form>
        );
    }
});

module.exports = DeleteAlarmCallbackButton;
