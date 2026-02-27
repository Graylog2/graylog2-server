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
import { ExpandedConfigurationSection, InputsActions } from 'components/inputs/InputsOveriew';

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

  const renderExpandedConfigurationDetails = useCallback(
    (input: InputSummary) => (
      <ExpandedConfigurationSection input={input} inputTypeDescriptions={inputTypeDescriptions} />
    ),
    [inputTypeDescriptions],
  );

  const expandedSections = useMemo(
    () => ({
      configuration: {
        title: 'Configuration',
        content: renderExpandedConfigurationDetails,
      },
    }),
    [renderExpandedConfigurationDetails],
  );

  return {
    entityActions,
    bulkActions: <>bulk</>,
    expandedSections,
  };
};

export default useTableElements;
