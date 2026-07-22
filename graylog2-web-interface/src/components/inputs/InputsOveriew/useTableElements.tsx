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
import MessagesPerStreamSection from 'components/inputs/InputsOveriew/expanded-sections/MessagesPerStreamSection';
import ExtractorsSection from 'components/inputs/InputsOveriew/expanded-sections/ExtractorsSection';
import ExtractorsSectionActions from 'components/inputs/InputsOveriew/expanded-sections/ExtractorsSectionActions';
import AssociatedStreamsSection from 'components/inputs/InputsOveriew/expanded-sections/AssociatedStreamsSection';
import { METRIC_COLUMN_IDS, METRIC_COLUMN_TITLES } from 'components/inputs/InputsOveriew/metricColumns';

const useTableElements = ({
  inputTypeDescriptions,
  inputTypes,
}: {
  inputTypes: InputTypesSummary;
  inputTypeDescriptions: InputTypeDescriptionsResponse;
}) => {
  const entityActions = useCallback(
    (listItem: Input) => (
      <InputsActions input={listItem} inputTypes={inputTypes} inputTypeDescriptions={inputTypeDescriptions} />
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
      [METRIC_COLUMN_IDS.messagesPerStream]: {
        title: METRIC_COLUMN_TITLES[METRIC_COLUMN_IDS.messagesPerStream],
        content: (input: InputSummary) => <MessagesPerStreamSection input={input} />,
      },
      [METRIC_COLUMN_IDS.extractorCount]: {
        title: METRIC_COLUMN_TITLES[METRIC_COLUMN_IDS.extractorCount],
        content: (input: InputSummary) => <ExtractorsSection input={input} />,
        actions: (input: InputSummary) => <ExtractorsSectionActions input={input} />,
      },
      [METRIC_COLUMN_IDS.associatedStreams]: {
        title: METRIC_COLUMN_TITLES[METRIC_COLUMN_IDS.associatedStreams],
        content: (input: InputSummary) => <AssociatedStreamsSection input={input} />,
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
