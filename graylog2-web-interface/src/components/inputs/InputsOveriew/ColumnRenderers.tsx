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

import TitleCell from './cells/TitleCell';
import TypeCell from './cells/TypeCell';
import NodeCell from './cells/NodeCell';
import ThroughputCell from './cells/ThroughputCell';
import ExpandedSectionToggleWrapper from './ExpandedSectionToggleWrapper';

import InputStateBadge from '../InputStateBadge';

type Props = {
  inputTypes: InputTypesSummary;
  inputStates?: InputStates;
};

const customColumnRenderers = ({ inputTypes, inputStates }: Props): ColumnRenderers<InputSummary> => ({
  attributes: {
    title: {
      renderCell: (_title: string, input: InputSummary) => (
        <ExpandedSectionToggleWrapper id={input.id}>
          <TitleCell input={input} />
        </ExpandedSectionToggleWrapper>
      ),
    },
    type: {
      renderCell: (type: string, input: InputSummary) => (
        <ExpandedSectionToggleWrapper id={input.id}>
          <TypeCell type={type} inputTypes={inputTypes} />
        </ExpandedSectionToggleWrapper>
      ),
    },
    state: {
      renderCell: (_state: string, input: InputSummary) => (
        <ExpandedSectionToggleWrapper id={input.id}>
          <InputStateBadge input={input} inputStates={inputStates} />
        </ExpandedSectionToggleWrapper>
      ),
    },
    node: {
      renderCell: (_type: string, input: InputSummary) => (
        <ExpandedSectionToggleWrapper id={input.id}>
          <NodeCell input={input} />
        </ExpandedSectionToggleWrapper>
      ),
    },
    traffic: {
      renderCell: (_traffic: string, input: InputSummary) => (
        <ExpandedSectionToggleWrapper id={input.id}>
          <ThroughputCell input={input} />
        </ExpandedSectionToggleWrapper>
      ),
    },
  },
});

export default customColumnRenderers;
