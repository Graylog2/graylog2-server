'use strict';

var React = require('react/addons');
var StreamRuleForm = require('./StreamRuleForm');
var PermissionsMixin = require('../../util/PermissionsMixin');

var StreamRule = React.createClass({
    mixins: [PermissionsMixin],
    _onDelete(evt) {},
    _onEdit(evt) {
        this.refs.streamRuleForm.open();
    },
    _formatActionItems() {
        return (
            <span>
                <a onClick={this._onDelete}>
                    <i className="fa fa-trash-o"></i>
                </a>


                <a onClick={this._onEdit}>
                    <i className="fa fa-edit"></i>
                </a>
            </span>
        );
    },
    render() {
        var streamRule = this.props.streamRule;
        var actionItems = (this.isPermitted(this.props.permissions, ['streams:edit:'+this.props.stream.id]) ? this._formatActionItems() : "");
        return (
            <li>
                {actionItems}{streamRule.field}
                <StreamRuleForm ref='streamRuleForm' streamRule={this.streamRule} title="Edit Stream Rule"/>
            </li>
        );
    }
});

module.exports = StreamRule;
