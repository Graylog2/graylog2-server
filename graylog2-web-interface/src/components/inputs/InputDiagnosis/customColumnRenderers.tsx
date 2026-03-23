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

import type { ColumnRenderers } from 'components/common/EntityDataTable';
import { Link } from 'components/common';
import Routes from 'routing/Routes';
import ConnectedPipelineLinkedCell from 'components/streams/StreamDetails/StreamDataRoutingIntake/cells/ConnectedPipelineLinkedCell';
import ConnectedPipelineStreamsCell from 'components/streams/StreamDetails/StreamDataRoutingIntake/cells/ConnectedPipelineStreamsCell';
import HumanReadableStreamRule from 'components/streamrules/HumanReadableStreamRule';
import { useStore } from 'stores/connect';
import { StreamRulesInputsStore } from 'stores/inputs/StreamRulesInputsStore';
import type { InputPipelineRule, InputStreamRule } from 'components/inputs/InputDiagnosis/types';

export const pipelineRulesColumnRenderers: ColumnRenderers<InputPipelineRule> = {
  attributes: {
    rule: {
      renderCell: (rule: InputPipelineRule['rule'], pipelineRule: InputPipelineRule) => (
        <ConnectedPipelineLinkedCell title={rule} id={pipelineRule.rule_id} type="rule" />
      ),
    },
    pipeline: {
      renderCell: (pipeline: InputPipelineRule['pipeline'], pipelineRule: InputPipelineRule) => (
        <ConnectedPipelineLinkedCell title={pipeline} id={pipelineRule.pipeline_id} type="pipeline" />
      ),
    },
    connected_streams: {
      renderCell: (connected_streams: InputPipelineRule['connected_streams']) => (
        <ConnectedPipelineStreamsCell streams={connected_streams} />
      ),
    },
  },
};

const StreamRuleCell = ({ streamRule }: { streamRule: InputStreamRule }) => {
  const { inputs } = useStore(StreamRulesInputsStore);

  return (
    <HumanReadableStreamRule
      streamRule={{
        type: streamRule.rule_type,
        value: streamRule.rule_value,
        field: streamRule.rule_field,
        inverted: streamRule.inverted,
      }}
      inputs={inputs}
    />
  );
};

export const streamRulesColumnRenderers: ColumnRenderers<InputStreamRule> = {
  attributes: {
    stream: {
      renderCell: (stream: InputStreamRule['stream'], streamRule: InputStreamRule) => (
        <Link to={Routes.stream_view(streamRule.stream_id)}>{stream}</Link>
      ),
    },
    rule: {
      renderCell: (_rule: string, streamRule: InputStreamRule) => <StreamRuleCell streamRule={streamRule} />,
    },
  },
};
