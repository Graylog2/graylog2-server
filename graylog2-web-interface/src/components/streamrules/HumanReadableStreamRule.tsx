/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
import { Trans, useTranslation } from 'react-i18next';

import i18n from 'i18n';
import type { Input } from 'components/messageloaders/Types';
import type { StreamRule } from 'logic/streams/types';
import STREAM_RULE_TYPES from 'logic/streams/streamRuleTypes';
import useStreamRuleTypes from 'components/streams/hooks/useStreamRuleTypes';

const getEmptyTag = () => i18n.t('stream_rules.human.empty', { ns: 'streams' });

const formatRuleValue = (inputs: Array<Input> | undefined, streamRule: Partial<StreamRule>) => {
  if (streamRule.type === STREAM_RULE_TYPES.MATCH_INPUT) {
    const input = inputs?.find(({ id }) => id === streamRule.value);

    if (input) {
      return `${input.title} (${input.name}: ${input.id})`;
    }

    return i18n.t('stream_rules.human.deleted_input', { ns: 'streams', value: streamRule.value });
  }

  if (streamRule.type !== STREAM_RULE_TYPES.FIELD_PRESENCE) {
    if (streamRule.value) {
      return streamRule.value;
    }

    return getEmptyTag();
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

  return getEmptyTag();
};

type Props = {
  streamRule: Partial<StreamRule>;
  inputs: Array<Input> | undefined;
};

const HumanReadableStreamRule = ({ streamRule, inputs }: Props) => {
  const { t } = useTranslation('streams');
  const { data: streamRuleTypes } = useStreamRuleTypes();
  const streamRuleType = streamRuleTypes?.find(({ id }) => id === streamRule.type);
  const longDesc = streamRuleType ? streamRuleType.long_desc : null;

  if (streamRule.type === STREAM_RULE_TYPES.ALWAYS_MATCHES) {
    return <span>{t('stream_rules.human.always_matches')}</span>;
  }

  return (
    <span>
      <Trans
        t={t}
        i18nKey={streamRule.inverted ? 'stream_rules.human.must_not' : 'stream_rules.human.must'}
        values={{
          field: formatRuleField(streamRule),
          description: longDesc ?? '',
          value: formatRuleValue(inputs, streamRule) ?? '',
        }}
        components={{ ruleField: <em />, mustPart: <strong />, ruleValue: <em /> }}
      />
    </span>
  );
};

export default HumanReadableStreamRule;
