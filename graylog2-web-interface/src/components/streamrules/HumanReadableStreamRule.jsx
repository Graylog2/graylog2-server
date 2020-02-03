import PropTypes from 'prop-types';
import React from 'react';

class HumanReadableStreamRule extends React.Component {
  EMPTY_TAG = '<empty>';

  FIELD_PRESENCE_RULE_TYPE = 5;

  ALWAYS_MATCH_RULE_TYPE = 7;

  static propTypes = {
    streamRule: PropTypes.object.isRequired,
    streamRuleTypes: PropTypes.array.isRequired,
  };

  _getTypeForInteger = (type, streamRuleTypes) => {
    if (streamRuleTypes) {
      return streamRuleTypes.filter((streamRuleType) => {
        return String(streamRuleType.id) === String(type);
      })[0];
    }
    return undefined;
  };

  _formatRuleValue = (streamRule) => {
    if (String(streamRule.type) !== String(this.FIELD_PRESENCE_RULE_TYPE)) {
      if (streamRule.value) {
        return streamRule.value;
      }
      return this.EMPTY_TAG;
    }

    return null;
  };

  _formatRuleField = (streamRule) => {
    if (streamRule.field) {
      return streamRule.field;
    }
    return this.EMPTY_TAG;
  };

  render() {
    const { streamRule, streamRuleTypes } = this.props;
    const streamRuleType = this._getTypeForInteger(streamRule.type, streamRuleTypes);
    const negation = (streamRule.inverted ? 'not ' : null);
    const longDesc = (streamRuleType ? streamRuleType.long_desc : null);
    if (String(streamRule.type) === String(this.ALWAYS_MATCH_RULE_TYPE)) {
      return (
        <span>Rule always matches</span>
      );
    }
    return (
      <span>
        <em>{this._formatRuleField(streamRule)}</em> <strong>must {negation}</strong>{longDesc} <em>{this._formatRuleValue(streamRule)}</em>
      </span>
    );
  }
}

export default HumanReadableStreamRule;
