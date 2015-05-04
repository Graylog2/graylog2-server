'use strict';

var React = require('react/addons');

var HumanReadableStreamRule = React.createClass({
    _getTypeForInteger(type, streamRuleTypes) {
        if (streamRuleTypes) {
            return streamRuleTypes.filter((streamRuleType) => {
                return streamRuleType.id == type;
            })[0];
        } else {
            return undefined;
        }
    },
    render() {
        var streamRule = this.props.streamRule;
        var streamRuleType = this._getTypeForInteger(streamRule.type, this.props.streamRuleTypes);
        var negation = (streamRule.inverted ? "not " : "");
        var valueDisplay = (streamRule.type != 5 ? <em>{streamRule.value}</em> : "");
        var longDesc = (streamRuleType ? streamRuleType.long_desc : "");
        return (
            <span><em>{streamRule.field}</em> must {negation}{longDesc} {valueDisplay}</span>
        );
    }
});

module.exports = HumanReadableStreamRule;
