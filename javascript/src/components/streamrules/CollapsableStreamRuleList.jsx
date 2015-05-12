'use strict';

var React = require('react/addons');
var StreamRuleList = require('./StreamRuleList');
var CollapsableMixin = require('react-bootstrap').CollapsableMixin;
var Alert = require('react-bootstrap').Alert;

var CollapsableStreamRuleList = React.createClass({
    mixins: [CollapsableMixin],
    getCollapsableDOMNode: function(){
        return this.refs.well.getDOMNode();
    },

    getCollapsableDimensionValue: function(){
        return this.refs.well.getDOMNode().scrollHeight;
    },
    onHandleToggle: function(e){
        e.preventDefault();
        this.setState({expanded:!this.state.expanded});
    },
    render() {
        var styles = this.getCollapsableClassSet();
        var text = this.isExpanded() ? 'Hide' : 'Show';

        return (
            <div>
                <a onClick={this.onHandleToggle}>{text} stream rules</a>
                <Alert ref='well' className={styles}>
                    <StreamRuleList stream={this.props.stream} streamRuleTypes={this.props.streamRuleTypes}
                                    permissions={this.props.permissions} />
                </Alert>
            </div>
        );
    }
});

module.exports = CollapsableStreamRuleList;
