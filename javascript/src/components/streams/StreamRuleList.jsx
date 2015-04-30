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
                return <StreamRule key={streamRule.id} streamRule={streamRule}/>;
            });
        } else {
            return <li>No rules defined.</li>;
        }
    },
    render() {
        var streamRules = this._formatStreamRules(this.props.streamRules);
        return (
            <ul className='streamrules-list'>
                {streamRules}
            </ul>
        );
    }
});

module.exports = StreamRuleList;
