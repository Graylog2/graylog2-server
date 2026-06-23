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
import React, { useState } from 'react';

import QueryHelper from 'components/common/QueryHelper';
import type { Stream } from 'logic/streams/types';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import { keyFn, fetchStreams } from 'components/streams/hooks/useStreams';
import getStreamTableElements from 'components/streams/StreamsOverview/Constants';
import FilterValueRenderers from 'components/streams/StreamsOverview/FilterValueRenderers';
import useTableElements from 'components/streams/StreamsOverview/hooks/useTableComponents';
import PaginatedEntityTable from 'components/common/PaginatedEntityTable';
import useUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUserLayoutPreferences';
import { ATTRIBUTE_STATUS } from 'components/common/EntityDataTable/Constants';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';
import type { SearchParams } from 'stores/PaginationTypes';
import type { PaginatedResponse } from 'components/common/PaginatedEntityTable/useFetchEntities';

import CustomColumnRenderers from './ColumnRenderers';
import usePipelineColumn from './hooks/usePipelineColumn';
import useStreamsOverviewExtensions from './hooks/useStreamsOverviewExtensions';
import { StreamMetricsProvider } from './StreamMetricsContext';
import { backendFieldsForVisibleColumns } from './metricColumns';

const streamIdsEqual = (first: Array<string>, second: Array<string>) =>
  first.length === second.length && first.every((id, index) => id === second[index]);

type Props = {
  indexSets: Array<IndexSet>;
};

const StreamsOverview = ({ indexSets }: Props) => {
  const { isPipelineColumnPermitted } = usePipelineColumn();
  const {
    columnRenderers: extensionColumnRenderers,
    attributes: extensionAttributes,
    expandedSections: pluggableExpandedSections,
    metricFields: extensionMetricFields,
  } = useStreamsOverviewExtensions();

  const { entityActions, expandedSections, bulkActions } = useTableElements({ indexSets, pluggableExpandedSections });

  const columnRenderers = CustomColumnRenderers(indexSets, isPipelineColumnPermitted, extensionColumnRenderers);
  const { additionalAttributes, defaultLayout } = getStreamTableElements(
    isPipelineColumnPermitted,
    extensionAttributes,
  );

  const fetchEntities = (options: SearchParams): Promise<PaginatedResponse<Stream>> => {
    CurrentUserStore.update(CurrentUserStore.getInitialState().currentUser.username);

    return fetchStreams(options);
  };

  const [visibleStreamIds, setVisibleStreamIds] = useState<Array<string>>([]);
  const onDataLoaded = (data: PaginatedResponse<Stream>) => {
    const nextVisibleStreamIds = data.list.map((entity) => entity.id);

    setVisibleStreamIds((currentVisibleStreamIds) =>
      streamIdsEqual(currentVisibleStreamIds, nextVisibleStreamIds) ? currentVisibleStreamIds : nextVisibleStreamIds,
    );
  };

  const { data: layoutPreferences } = useUserLayoutPreferences(defaultLayout.entityTableId);
  const userPrefs = layoutPreferences?.attributes ?? {};
  const userSelection = Object.entries(userPrefs)
    .filter(([, pref]) => pref.status === ATTRIBUTE_STATUS.show)
    .map(([attributeId]) => attributeId);
  const visibleColumns = userSelection.length > 0 ? userSelection : defaultLayout.defaultDisplayedAttributes;
  const requestedFields = backendFieldsForVisibleColumns(visibleColumns, extensionMetricFields);

  return (
    <StreamMetricsProvider streamIds={visibleStreamIds} fields={requestedFields}>
      <PaginatedEntityTable<Stream>
        humanName="streams"
        additionalAttributes={additionalAttributes}
        queryHelpComponent={<QueryHelper entityName="stream" />}
        entityActions={entityActions}
        tableLayout={defaultLayout}
        fetchEntities={fetchEntities}
        onDataLoaded={onDataLoaded}
        keyFn={keyFn}
        expandedSectionRenderers={expandedSections}
        bulkSelection={{ actions: bulkActions }}
        entityAttributesAreCamelCase={false}
        filterValueRenderers={FilterValueRenderers}
        columnRenderers={columnRenderers}
      />
    </StreamMetricsProvider>
  );
};

export default StreamsOverview;
