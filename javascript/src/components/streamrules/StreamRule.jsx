'use strict';

var React = require('react/addons');
var StreamRuleForm = require('./StreamRuleForm');
var PermissionsMixin = require('../../util/PermissionsMixin');
var HumanReadableStreamRule = require('./HumanReadableStreamRule');

var StreamRule = React.createClass({
    mixins: [PermissionsMixin],
    _onEdit(evt) {
        this.refs.streamRuleForm.open();
    },
    _formatActionItems() {
        return (
            <span>
                <a onClick={this.props.onDelete.bind(null, this.props.streamRule.id)}>
                    <i className="fa fa-trash-o"></i>
                </a>


                <a onClick={this._onEdit}>
                    <i className="fa fa-edit"></i>
                </a>
            </span>
        );
    },
    _getMatchDataClassNames() {
        return (this.props.matchData.rules[this.props.streamRule.id] ? "alert-success" : "alert-danger");
    },
    render() {
        var streamRule = this.props.streamRule;
        var streamRuleTypes = this.props.streamRuleTypes;
        var actionItems = (this.isPermitted(this.props.permissions, ['streams:edit:'+this.props.stream.id]) ? this._formatActionItems() : "");
        var className = (this.props.matchData ? this._getMatchDataClassNames() : "");
        return (
            <li className={className}>
                {actionItems} <HumanReadableStreamRule streamRule={streamRule} streamRuleTypes={streamRuleTypes} />
                <StreamRuleForm ref='streamRuleForm' streamRule={streamRule} streamRuleTypes={streamRuleTypes}
                                title="Edit Stream Rule" onSubmit={this.props.onSubmit}/>
            </li>
        );
    }
});

module.exports = StreamRule;
