import React from 'react';

const HumanReadableStreamRule = React.createClass({
  propTypes: {
    streamRule: React.PropTypes.object.isRequired,
    streamRuleTypes: React.PropTypes.array.isRequired,
  },
  EMPTY_TAG: '<empty>',
  FIELD_PRESENCE_RULE_TYPE: 5,
  ALWAYS_MATCH_RULE_TYPE: 7,
  _getTypeForInteger(type, streamRuleTypes) {
    if (streamRuleTypes) {
      return streamRuleTypes.filter((streamRuleType) => {
        return String(streamRuleType.id) === String(type);
      })[0];
    }
    return undefined;
  },
  _formatRuleValue(streamRule) {
    if (String(streamRule.type) !== String(this.FIELD_PRESENCE_RULE_TYPE)) {
      if (streamRule.value) {
        return streamRule.value;
      }
      return this.EMPTY_TAG;
    }

    return null;
  },
  _formatRuleField(streamRule) {
    if (streamRule.field) {
      return streamRule.field;
    }
    return this.EMPTY_TAG;
  },
  render() {
    const streamRule = this.props.streamRule;
    const streamRuleType = this._getTypeForInteger(streamRule.type, this.props.streamRuleTypes);
    const negation = (streamRule.inverted ? 'not ' : null);
    const longDesc = (streamRuleType ? streamRuleType.long_desc : null);
    if (String(streamRule.type) === String(this.ALWAYS_MATCH_RULE_TYPE)) {
      return (
        <span>Rule always matches</span>
      );
    }
    return (
      <span>Field <em>{this._formatRuleField(streamRule)}</em> must {negation}{longDesc} <em>{this._formatRuleValue(streamRule)}</em></span>
    );
  },
});

export default HumanReadableStreamRule;
