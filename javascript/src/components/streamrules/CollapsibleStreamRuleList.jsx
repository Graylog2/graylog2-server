'use strict';

var React = require('react/addons');
var StreamRuleList = require('./StreamRuleList');
var CollapsibleMixin = require('react-bootstrap').CollapsibleMixin;
var Alert = require('react-bootstrap').Alert;

var CollapsibleStreamRuleList = React.createClass({
    mixins: [CollapsibleMixin],
    getCollapsibleDOMNode: function(){
        return React.findDOMNode(this.refs.well);
    },

    getCollapsibleDimensionValue: function(){
        return React.findDOMNode(this.refs.well).scrollHeight;
    },
    onHandleToggle: function(e){
        e.preventDefault();
        this.setState({expanded:!this.state.expanded});
    },
    render() {
        var styles = this.getCollapsibleClassSet();
        var text = this.isExpanded() ? 'Hide' : 'Show';

        return (
            <span className="stream-rules-link">
                <a href="#" onClick={this.onHandleToggle}>{text} stream rules</a>
                <Alert ref='well' className={styles}>
                    <StreamRuleList stream={this.props.stream} streamRuleTypes={this.props.streamRuleTypes}
                                    permissions={this.props.permissions} />
                </Alert>
            </span>
        );
    }
});

module.exports = CollapsibleStreamRuleList;
