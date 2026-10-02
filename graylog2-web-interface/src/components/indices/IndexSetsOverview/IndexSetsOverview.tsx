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

import PaginatedEntityTable from 'components/common/PaginatedEntityTable';
import useLayoutVariant from 'components/common/PaginatedEntityTable/hooks/useLayoutVariant';

import getIndexSetTableElements, { DETAILS_SECTION, INDEX_SET_VIEW_VARIANTS } from './Constants';
import customColumnRenderers from './ColumnRenderers';
import IndexSetActions from './IndexSetActions';
import IndexSetViewButtons from './IndexSetViewButtons';
import IndexSetDetailsSection from './expanded-sections/IndexSetDetailsSection';
import { fetchIndexSets, keyFn } from './fetchIndexSets';
import useIndexSetsOverviewExtensions from './hooks/useIndexSetsOverviewExtensions';
import type { IndexSetEntity } from './types';

const expandedSections = {
  [DETAILS_SECTION]: {
    title: 'Details',
    content: (indexSet: IndexSetEntity) => <IndexSetDetailsSection indexSet={indexSet} />,
  },
};

const renderActions = (indexSet: IndexSetEntity) => <IndexSetActions indexSet={indexSet} />;

const IndexSetsOverview = () => {
  const {
    columnRenderers: extensionColumnRenderers,
    attributes: extensionAttributes,
    columnGroups: extensionColumnGroups,
  } = useIndexSetsOverviewExtensions();
  const { defaultVariantLayout, configurationVariantLayout, additionalAttributes } = getIndexSetTableElements(
    extensionAttributes,
    extensionColumnGroups,
  );
  const { activeLayoutVariant } = useLayoutVariant();
  const activeLayout =
    activeLayoutVariant === INDEX_SET_VIEW_VARIANTS.configuration ? configurationVariantLayout : defaultVariantLayout;

  return (
    <PaginatedEntityTable<IndexSetEntity>
      humanName="index sets"
      searchPlaceholder="Find index sets"
      additionalAttributes={additionalAttributes}
      entityActions={renderActions}
      tableLayout={activeLayout}
      fetchEntities={fetchIndexSets}
      keyFn={keyFn}
      expandedSectionRenderers={expandedSections}
      entityAttributesAreCamelCase={false}
      columnRenderers={customColumnRenderers(extensionColumnRenderers)}
      topSection={IndexSetViewButtons}
    />
  );
};

export default IndexSetsOverview;
