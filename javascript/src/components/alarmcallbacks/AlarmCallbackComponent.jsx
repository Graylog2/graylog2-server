'use strict';

var React = require('react/addons');
var AlarmCallbackList = require('./AlarmCallbackList');

var AlarmCallbackComponent = React.createClass({
    getInitialState() {
        return {
            permissions: JSON.parse(this.props.permissions),
            streamId: this.props.streamId
        };
    },
    render() {
        return (
            <AlarmCallbackList streamId={this.state.streamId} permissions={this.state.permissions} />
        );
    }
});

module.exports = AlarmCallbackComponent;
