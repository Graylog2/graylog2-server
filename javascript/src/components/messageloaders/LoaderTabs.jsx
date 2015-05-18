'use strict';

var React = require('react/addons');
var TabbedArea = require('react-bootstrap').TabbedArea;
var TabPane = require('react-bootstrap').TabPane;
var RecentMessageLoader = require('./RecentMessageLoader');
var MessageShow = require('../search/MessageShow');
var InputsStore = require('../../stores/inputs/InputsStore');
var Immutable = require('immutable');
var MessageLoader = require('../extractors/MessageLoader');

var LoaderTabs = React.createClass({
    getInitialState() {
        return {
            message: undefined,
            inputs: undefined
        };
    },
    onMessageLoaded(message) {
        message['formatted_fields'] = message.fields;
        message.fields["_id"] = message.id;
        this.setState({message: message});
        if (this.props.onMessageLoaded) {
            this.props.onMessageLoaded(message);
        }
    },
    loadData() {
        InputsStore.list((inputsList) => {
            var inputs = {};
            for(var idx in inputsList) {
                var input = inputsList[idx];
                inputs[input.id] = input;
            }
            this.setState({inputs: Immutable.Map(inputs)});
        });
    },
    componentDidMount() {
        this.loadData();
        var messageId = this.props.messageId;
        var index = this.props.index;
        if (messageId && index) {
            this.refs.messageLoader.submit(messageId, index);
        }
    },
    render() {
        var displayMessage = (this.state.message && this.state.inputs ?
            <MessageShow message={this.state.message} inputs={this.state.inputs}
                         disableTestAgainstStream={true} disableFieldActions={true}/> : null);
        var defaultActiveKey;
        if (this.props.messageId && this.props.index) {
            defaultActiveKey = 2;
        } else {
            defaultActiveKey = 1;
        }
        return (
            <div>
                <TabbedArea defaultActiveKey={defaultActiveKey}>
                    <TabPane eventKey={1} tab='Recent'>
                        <RecentMessageLoader inputs={this.state.inputs} onMessageLoaded={this.onMessageLoaded}/>
                    </TabPane>
                    <TabPane eventKey={2} tab='Manual'>
                        Please provide the id and index of the message that you want to load in this form:
                        <MessageLoader ref="messageLoader" onMessageLoaded={this.onMessageLoaded} hidden={false} hideText={true}/>
                    </TabPane>
                </TabbedArea>
                {displayMessage}
            </div>
        );
    }
});

module.exports = LoaderTabs;
