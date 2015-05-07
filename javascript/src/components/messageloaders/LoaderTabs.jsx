'use strict';

var React = require('react/addons');
var TabbedArea = require('react-bootstrap').TabbedArea;
var TabPane = require('react-bootstrap').TabPane;
var RecentMessageLoader = require('./RecentMessageLoader');
var MessageShow = require('../search/MessageShow');
var InputsStore = require('../../stores/inputs/InputsStore');
var Immutable = require('immutable');

var LoaderTabs = React.createClass({
    getInitialState() {
        return {
            message: undefined,
            inputs: undefined
        };
    },
    onMessageLoaded(message) {
        this.setState({message: message});
    },
    loadData() {
        InputsStore.list((inputsList) => {
            var inputs = {};
            for(var idx in inputsList) {
                var input = inputsList[idx];
                inputs[input.id] = input;
            }
            this.setState({inputs: Immutable.Map(inputs),
                            nodes: Immutable.Map({"unknown": {}})});
        });
    },
    componentDidMount() {
        this.loadData();
    },
    render() {
        var displayMessage = (this.state.message && this.state.inputs ?
            <MessageShow message={this.state.message} inputs={this.state.inputs} nodes={this.state.nodes}/> : "");
        return (
            <div>
                <TabbedArea defaultActiveKey={1}>
                    <TabPane eventKey={1} tab='Recent'>
                        <RecentMessageLoader onMessageLoaded={this.onMessageLoaded}/>
                    </TabPane>
                    <TabPane eventKey={2} tab='Manual'>TabPane 2 content</TabPane>
                </TabbedArea>
                {displayMessage}
            </div>
        );
    }
});

module.exports = LoaderTabs;
