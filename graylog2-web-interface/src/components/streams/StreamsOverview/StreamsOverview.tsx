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
import getStreamTableElements, { STREAM_VIEW_VARIANTS } from 'components/streams/StreamsOverview/Constants';
import FilterValueRenderers from 'components/streams/StreamsOverview/FilterValueRenderers';
import useTableElements from 'components/streams/StreamsOverview/hooks/useTableComponents';
import PaginatedEntityTable from 'components/common/PaginatedEntityTable';
import useUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUserLayoutPreferences';
import useLayoutVariant from 'components/common/PaginatedEntityTable/hooks/useLayoutVariant';
import { ATTRIBUTE_STATUS } from 'components/common/EntityDataTable/Constants';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';
import type { SearchParams } from 'stores/PaginationTypes';
import type { PaginatedResponse } from 'components/common/PaginatedEntityTable/useFetchEntities';

import CustomColumnRenderers from './ColumnRenderers';
import usePipelineColumn from './hooks/usePipelineColumn';
import useStreamsOverviewExtensions from './hooks/useStreamsOverviewExtensions';
import StreamViewButtons from './StreamViewButtons';
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
    columnGroups: extensionColumnGroups,
    expandedSections: pluggableExpandedSections,
    metricFields: extensionMetricFields,
  } = useStreamsOverviewExtensions();

  const { entityActions, expandedSections, bulkActions } = useTableElements({ indexSets, pluggableExpandedSections });

  const columnRenderers = CustomColumnRenderers(indexSets, isPipelineColumnPermitted, extensionColumnRenderers);
  const { defaultVariantLayout, routingVariantLayout, performanceVariantLayout, additionalAttributes } =
    getStreamTableElements(isPipelineColumnPermitted, extensionAttributes, extensionColumnGroups);

  const { activeLayoutVariant } = useLayoutVariant();

  const variantLayouts: Record<string, typeof defaultVariantLayout> = {
    [STREAM_VIEW_VARIANTS.routing]: routingVariantLayout,
    [STREAM_VIEW_VARIANTS.performance]: performanceVariantLayout,
  };
  const activeLayout = variantLayouts[activeLayoutVariant] ?? defaultVariantLayout;

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

  const { data: layoutPreferences } = useUserLayoutPreferences(activeLayout.entityTableId, activeLayoutVariant);
  const userPrefs = layoutPreferences?.attributes ?? {};
  const userSelection = Object.entries(userPrefs)
    .filter(([, pref]) => pref.status === ATTRIBUTE_STATUS.show)
    .map(([attributeId]) => attributeId);
  const visibleColumns = userSelection.length > 0 ? userSelection : activeLayout.defaultDisplayedAttributes;
  const requestedFields = backendFieldsForVisibleColumns(visibleColumns, extensionMetricFields);

  return (
    <StreamMetricsProvider streamIds={visibleStreamIds} fields={requestedFields}>
      <PaginatedEntityTable<Stream>
        humanName="streams"
        additionalAttributes={additionalAttributes}
        queryHelpComponent={<QueryHelper entityName="stream" />}
        entityActions={entityActions}
        tableLayout={activeLayout}
        fetchEntities={fetchEntities}
        onDataLoaded={onDataLoaded}
        keyFn={keyFn}
        expandedSectionRenderers={expandedSections}
        bulkSelection={{ actions: bulkActions }}
        entityAttributesAreCamelCase={false}
        filterValueRenderers={FilterValueRenderers}
        columnRenderers={columnRenderers}
        topSection={StreamViewButtons}
      />
    </StreamMetricsProvider>
  );
};

export default StreamsOverview;
