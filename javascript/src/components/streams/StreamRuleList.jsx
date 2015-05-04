'use strict';

var React = require('react/addons');
var StreamRule = require('./StreamRule');

var StreamRuleList = React.createClass({
    getInitialState() {
        return {};
    },
    _formatStreamRules(streamRules) {
        if (streamRules.length > 0) {
            return streamRules.map((streamRule) => {
                return <StreamRule key={streamRule.id} permissions={this.props.permissions}
                                   stream={this.props.stream} streamRule={streamRule}/>;
            });
        } else {
            return <li>No rules defined.</li>;
        }
    },
    render() {
        var streamRules = this._formatStreamRules(this.props.stream.stream_rules);
        return (
            <ul className='streamrules-list'>
                {streamRules}
            </ul>
        );
    }
});

module.exports = StreamRuleList;
