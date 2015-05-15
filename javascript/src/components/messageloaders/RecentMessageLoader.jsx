'use strict';

var React = require('react/addons');
var InputDropdown = require('../inputs/InputDropdown');
var InputsStore = require('../../stores/inputs/InputsStore');
var UserNotification = require("../../util/UserNotification");

var RecentMessageLoader = React.createClass({
    onClick(inputId) {
        var input = this.props.inputs.get(inputId);
        if (!input) {
            UserNotification.error("Invalid input selected: " + inputId,
                "Could not load message from invalid Input " + inputId);
        }
        InputsStore.globalRecentMessage(input, (message) => {
            message['source_input_id'] = input.id;
            this.props.onMessageLoaded(message);
        });
    },
    render() {
        return (
            <div>
                Select an Input from the list below and click "Load Message" to load the most recent message from this input.
                <InputDropdown inputs={this.props.inputs} onClick={this.onClick} title="Load Message"/>
            </div>
        );
    }
});

module.exports = RecentMessageLoader;
