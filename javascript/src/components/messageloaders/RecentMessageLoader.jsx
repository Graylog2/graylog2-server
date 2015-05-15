'use strict';

var React = require('react/addons');
var InputDropdown = require('../inputs/InputDropdown');
var InputsStore = require('../../stores/inputs/InputsStore');

var RecentMessageLoader = React.createClass({
    onClick(inputId) {
        InputsStore.globalRecentMessage(inputId, (message) => {
            message['source_input_id'] = inputId;
            message['source_node_id'] = 'unknown';
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
