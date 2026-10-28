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
import * as React from 'react';

import type { ColumnRenderers } from 'components/common/EntityDataTable';
import type { InputSummary } from 'hooks/usePaginatedInputs';
import type { InputTypesSummary } from 'hooks/useInputTypes';
import type { InputStates } from 'hooks/useInputsStates';
import { TypeCell, NodeCell, ThroughputCell } from 'components/inputs/InputsOveriew';
import FailuresCell from 'components/inputs/InputsOveriew/cells/FailuresCell';
import MessageCountCell from 'components/inputs/InputsOveriew/cells/MessageCountCell';
import ExtractorCountCell from 'components/inputs/InputsOveriew/cells/ExtractorCountCell';
import AssociatedStreamsCell from 'components/inputs/InputsOveriew/cells/AssociatedStreamsCell';
import { METRIC_COLUMN_IDS } from 'components/inputs/InputsOveriew/metricColumns';
import { InputStateBadge } from 'components/inputs';
import Routes from 'routing/Routes';
import { Link } from 'components/common';
import { COLUMN_WIDTH } from 'components/common/EntityDataTable/Constants';

type Props = {
  inputTypes: InputTypesSummary;
  inputStates?: InputStates;
};

const customColumnRenderers = ({ inputTypes, inputStates }: Props): ColumnRenderers<InputSummary> => ({
  attributes: {
    title: {
      renderCell: (_title: string, input: InputSummary) => (
        <Link to={Routes.SYSTEM.INPUT_DIAGNOSIS(input.id)} title={`show input diagnosis for ${input.title}`}>
          {input.title}
        </Link>
      ),
    },
    type: {
      renderCell: (type: string) => <TypeCell type={type} inputTypes={inputTypes} />,
    },
    desired_state: {
      renderCell: (_state: string, input: InputSummary) => <InputStateBadge input={input} inputStates={inputStates} />,
      staticWidth: 130,
    },
    node_id: {
      renderCell: (_type: string, input: InputSummary) => <NodeCell input={input} />,
      staticWidth: COLUMN_WIDTH.reference,
    },
    traffic: {
      renderCell: (_traffic: string, input: InputSummary) => <ThroughputCell input={input} />,
      staticWidth: 'matchHeader',
      textAlign: 'right',
    },
    input_failures: {
      renderCell: (_failures: string, input: InputSummary) => <FailuresCell input={input} />,
      staticWidth: 'matchHeader',
      textAlign: 'right',
    },
    address: {
      renderCell: (_address: string, input: InputSummary) => input.attributes?.bind_address || 'N/A',
      staticWidth: 125,
    },
    port: {
      renderCell: (_port: string, input: InputSummary) => input.attributes?.port || 'N/A',
      staticWidth: 'matchHeader',
      textAlign: 'right',
    },
    [METRIC_COLUMN_IDS.messagesPerStream]: {
      renderCell: (_value: unknown, input: InputSummary) => <MessageCountCell input={input} />,
      staticWidth: 'matchHeader',
      textAlign: 'right',
    },
    [METRIC_COLUMN_IDS.extractorCount]: {
      renderCell: (_value: unknown, input: InputSummary) => <ExtractorCountCell input={input} />,
      staticWidth: 'matchHeader',
    },
    [METRIC_COLUMN_IDS.associatedStreams]: {
      renderCell: (_value: unknown, input: InputSummary) => <AssociatedStreamsCell input={input} />,
      staticWidth: 'matchHeader',
      textAlign: 'right',
    },
  },
});

export default customColumnRenderers;
