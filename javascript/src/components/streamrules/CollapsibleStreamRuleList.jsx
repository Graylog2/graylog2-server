'use strict';

var React = require('react');
var StreamRuleList = require('./StreamRuleList');
var Collapse = require('react-bootstrap').Collapse;
var Alert = require('react-bootstrap').Alert;

var CollapsibleStreamRuleList = React.createClass({
    getInitialState() {
        return {
            expanded: false,
        };
    },
    onHandleToggle: function(e){
        e.preventDefault();
        this.setState({expanded:!this.state.expanded});
    },
    render() {
        var text = this.state.expanded ? 'Hide' : 'Show';

        return (
            <span className="stream-rules-link">
                <a href="#" onClick={this.onHandleToggle}>{text} stream rules</a>
                <Collapse in={this.state.expanded} timeout={0}>
                    <Alert ref='well'>
                        <StreamRuleList stream={this.props.stream} streamRuleTypes={this.props.streamRuleTypes}
                                        permissions={this.props.permissions} />
                    </Alert>
                </Collapse>
            </span>
        );
    }
});

module.exports = CollapsibleStreamRuleList;
