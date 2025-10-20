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
import React, { useCallback, useMemo } from 'react';

import type { Input } from 'components/messageloaders/Types';
import type { InputTypesSummary } from 'hooks/useInputTypes';
import type { InputTypeDescriptionsResponse } from 'hooks/useInputTypesDescriptions';
import type { InputSummary } from 'hooks/usePaginatedInputs';

import ExpandedThroughputSection from './expanded-sections/ExpandedThroughputSection';
import InputsActions from './InputsActions';
import ExpandedTitleSection from './expanded-sections/ExpandedTitleSection';

const useTableElements = ({
  inputTypeDescriptions,
  inputTypes,
}: {
  inputTypes: InputTypesSummary;
  inputTypeDescriptions: InputTypeDescriptionsResponse;
}) => {
  const entityActions = useCallback(
    (listItem: Input) => (
      <InputsActions
        input={listItem}
        inputTypes={inputTypes}
        inputTypeDescriptions={inputTypeDescriptions}
        currentNode={null}
      />
    ),
    [inputTypes, inputTypeDescriptions],
  );

  const renderExpandedThroughput = useCallback(
    (input: InputSummary) => <ExpandedThroughputSection input={input} />,
    [],
  );
  const renderExpandedInputDetails = useCallback(
    (input: InputSummary) => <ExpandedTitleSection input={input} inputTypeDescriptions={inputTypeDescriptions} />,
    [inputTypeDescriptions],
  );
  const renderExpandedRulesActions = useCallback((_input: Input) => <>x</>, []);
  const expandedSections = useMemo(
    () => ({
      traffic: {
        title: 'Throughput',
        content: renderExpandedThroughput,
        actions: renderExpandedRulesActions,
      },
      title: {
        title: 'Configuration',
        content: renderExpandedInputDetails,
      },
    }),
    [renderExpandedThroughput, renderExpandedRulesActions, renderExpandedInputDetails],
  );

  return {
    entityActions,
    bulkActions: <>bulk</>,
    expandedSections,
  };
};

export default useTableElements;
