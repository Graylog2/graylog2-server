'use strict';

var React = require('react/addons');
var StreamRule = require('./StreamRule');

var StreamRuleList = React.createClass({
    _formatStreamRules(streamRules) {
        if (streamRules && streamRules.length > 0) {
            return streamRules.map((streamRule) => {
                return <StreamRule key={streamRule.id} permissions={this.props.permissions} matchData={this.props.matchData}
                                   onSubmit={this.props.onSubmit} onDelete={this.props.onDelete}
                                   stream={this.props.stream} streamRule={streamRule} streamRuleTypes={this.props.streamRuleTypes}/>;
            });
        } else {
            return <li>No rules defined.</li>;
        }
    },
    render() {
        if (this.props.stream) {
            var streamRules = this._formatStreamRules(this.props.stream.stream_rules);
            return (
                <ul className="streamrules-list">
                    {streamRules}
                </ul>
            );
        } else {
            return (
                <div><i className="fa fa-spin fa-spinner"/> Loading</div>
            );
        }
    }
});

module.exports = StreamRuleList;
