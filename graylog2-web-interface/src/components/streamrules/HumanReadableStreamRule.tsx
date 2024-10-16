import React from 'react';

import type { Input } from 'components/messageloaders/Types';
import type { StreamRule } from 'stores/streams/StreamsStore';
import STREAM_RULE_TYPES from 'logic/streams/streamRuleTypes';
import useStreamRuleTypes from 'components/streams/hooks/useStreamRuleTypes';

const EMPTY_TAG = '<empty>';

const formatRuleValue = (inputs: Array<Input>, streamRule: Partial<StreamRule>) => {
  if (streamRule.type === STREAM_RULE_TYPES.MATCH_INPUT) {
    const input = inputs.find(({ id }) => id === streamRule.value);

    if (input) {
      return `${input.title} (${input.name}: ${input.id})`;
    }

    return `<deleted input>: ${streamRule.value})`;
  }

  if (streamRule.type !== STREAM_RULE_TYPES.FIELD_PRESENCE) {
    if (streamRule.value) {
      return streamRule.value;
    }

    return EMPTY_TAG;
  }

  return null;
};

const formatRuleField = (streamRule: Partial<StreamRule>) => {
  if (streamRule.field) {
    return streamRule.field;
  }

  if (streamRule.type === STREAM_RULE_TYPES.MATCH_INPUT) {
    return 'gl_source_input';
  }

  return EMPTY_TAG;
};

type Props = {
  streamRule: Partial<StreamRule>,
  inputs: Array<Input>,
}

const HumanReadableStreamRule = ({ streamRule, inputs = [] }: Props) => {
  const { data: streamRuleTypes } = useStreamRuleTypes();
  const streamRuleType = streamRuleTypes?.find(({ id }) => id === streamRule.type);
  const negation = (streamRule.inverted ? 'not ' : null);
  const longDesc = (streamRuleType ? streamRuleType.long_desc : null);

  if (streamRule.type === STREAM_RULE_TYPES.ALWAYS_MATCHES) {
    return (
      <span>Rule always matches</span>
    );
  }

  return (
    <span>
      <em>{formatRuleField(streamRule)}</em> <strong>must {negation}</strong>{longDesc} <em>{formatRuleValue(inputs, streamRule)}</em>
    </span>
  );
};

export default HumanReadableStreamRule;
