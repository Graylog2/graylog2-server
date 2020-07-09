import PropTypes from 'prop-types';
import React from 'react';

class HumanReadableStreamRule extends React.Component {
  EMPTY_TAG = '<empty>';

  FIELD_PRESENCE_RULE_TYPE = 5;

  ALWAYS_MATCH_RULE_TYPE = 7;

  MATCH_INPUT = 8;

  _getTypeForInteger = (type, streamRuleTypes) => {
    if (streamRuleTypes) {
      return streamRuleTypes.filter((streamRuleType) => {
        return String(streamRuleType.id) === String(type);
      })[0];
    }

    return undefined;
  };

  _inputName = (inputId) => {
    const { inputs } = this.props;

    return inputs.find((input) => input.id === inputId);
  }

  _formatRuleValue = (streamRule) => {
    if (String(streamRule.type) === String(this.MATCH_INPUT)) {
      const input = this._inputName(streamRule.value);

      if (input) {
        return `${input.title} (${input.name}: ${input.id})`;
      }
    }

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

    if (String(streamRule.type) === String(this.MATCH_INPUT)) {
      return 'gl_source_input';
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

HumanReadableStreamRule.propTypes = {
  streamRule: PropTypes.object.isRequired,
  streamRuleTypes: PropTypes.array.isRequired,
  inputs: PropTypes.array,
};

HumanReadableStreamRule.defaultProps = {
  inputs: [],
};

export default HumanReadableStreamRule;
