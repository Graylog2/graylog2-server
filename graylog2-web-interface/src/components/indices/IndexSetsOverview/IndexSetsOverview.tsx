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
import { useState } from 'react';

import PaginatedEntityTable from 'components/common/PaginatedEntityTable';
import useLayoutVariant from 'components/common/PaginatedEntityTable/hooks/useLayoutVariant';
import type { PaginatedResponse } from 'components/common/PaginatedEntityTable/useFetchEntities';
import useUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUserLayoutPreferences';
import { ATTRIBUTE_STATUS } from 'components/common/EntityDataTable/Constants';

import getIndexSetTableElements, { DETAILS_SECTION, INDEX_SET_VIEW_VARIANTS } from './Constants';
import customColumnRenderers from './ColumnRenderers';
import IndexSetActions from './IndexSetActions';
import IndexSetCategoryButtons from './IndexSetCategoryButtons';
import IndexSetViewButtons from './IndexSetViewButtons';
import IndexSetDetailsSection from './expanded-sections/IndexSetDetailsSection';
import { fetchIndexSets, keyFn } from './fetchIndexSets';
import useIndexSetsOverviewExtensions from './hooks/useIndexSetsOverviewExtensions';
import { IndexSetMetricsProvider } from './IndexSetMetricsContext';
import { backendFieldsForVisibleColumns } from './metricColumns';
import type { IndexSetEntity } from './types';

const expandedSections = {
  [DETAILS_SECTION]: {
    title: 'Details',
    content: (indexSet: IndexSetEntity) => <IndexSetDetailsSection indexSet={indexSet} />,
  },
};

const TopSection = () => (
  <>
    <IndexSetCategoryButtons />
    <IndexSetViewButtons />
  </>
);

const renderActions = (indexSet: IndexSetEntity) => <IndexSetActions indexSet={indexSet} />;

const idsEqual = (first: Array<string>, second: Array<string>) =>
  first.length === second.length && first.every((id, index) => id === second[index]);

const IndexSetsOverview = () => {
  const {
    columnRenderers: extensionColumnRenderers,
    attributes: extensionAttributes,
    columnGroups: extensionColumnGroups,
  } = useIndexSetsOverviewExtensions();
  const { defaultVariantLayout, configurationVariantLayout, routingVariantLayout, additionalAttributes } =
    getIndexSetTableElements(extensionAttributes, extensionColumnGroups);
  const { activeLayoutVariant } = useLayoutVariant();
  const variantLayouts: Record<string, typeof defaultVariantLayout> = {
    [INDEX_SET_VIEW_VARIANTS.configuration]: configurationVariantLayout,
    [INDEX_SET_VIEW_VARIANTS.routing]: routingVariantLayout,
  };
  const activeLayout = variantLayouts[activeLayoutVariant] ?? defaultVariantLayout;

  const [visibleIndexSetIds, setVisibleIndexSetIds] = useState<Array<string>>([]);
  const onDataLoaded = (data: PaginatedResponse<IndexSetEntity>) => {
    const nextIds = data.list.map(({ id }) => id);

    setVisibleIndexSetIds((currentIds) => (idsEqual(currentIds, nextIds) ? currentIds : nextIds));
  };

  const { data: layoutPreferences } = useUserLayoutPreferences(
    activeLayout.entityTableId,
    activeLayoutVariant || undefined,
  );
  const userSelection = Object.entries(layoutPreferences?.attributes ?? {})
    .filter(([, pref]) => pref.status === ATTRIBUTE_STATUS.show)
    .map(([attributeId]) => attributeId);
  const visibleColumns = userSelection.length > 0 ? userSelection : activeLayout.defaultDisplayedAttributes;

  return (
    <IndexSetMetricsProvider indexSetIds={visibleIndexSetIds} fields={backendFieldsForVisibleColumns(visibleColumns)}>
      <PaginatedEntityTable<IndexSetEntity>
        humanName="index sets"
        searchPlaceholder="Find index sets"
        additionalAttributes={additionalAttributes}
        entityActions={renderActions}
        tableLayout={activeLayout}
        fetchEntities={fetchIndexSets}
        onDataLoaded={onDataLoaded}
        keyFn={keyFn}
        expandedSectionRenderers={expandedSections}
        entityAttributesAreCamelCase={false}
        columnRenderers={customColumnRenderers(extensionColumnRenderers)}
        topSection={TopSection}
      />
    </IndexSetMetricsProvider>
  );
};

export default IndexSetsOverview;
