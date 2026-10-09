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
import SetProfileModal from 'components/indices/IndexSetFieldTypes/SetProfileModal';
import { appliedIndexSetIds } from 'components/indices/IndexSetFieldTypes/profileChangeResult';
import type { ProfileChangeResponse } from 'components/indices/IndexSetFieldTypes/types';

import getIndexSetTableElements, { DETAILS_SECTION, INDEX_SET_VIEW_VARIANTS } from './Constants';
import customColumnRenderers from './ColumnRenderers';
import BulkActions from './BulkActions';
import type { ProfileChange } from './BulkActions';
import IndexSetActions from './IndexSetActions';
import IndexSetCategoryButtons from './IndexSetCategoryButtons';
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

const TopSection = () => (
  <>
    <IndexSetCategoryButtons />
    <IndexSetViewButtons />
  </>
);

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

  const [indexSetsById, setIndexSetsById] = useState<Record<string, IndexSetEntity>>({});
  const [profileChange, setProfileChange] = useState<ProfileChange | null>(null);
  const onDataLoaded = (data: PaginatedResponse<IndexSetEntity>) =>
    setIndexSetsById((current) =>
      data.list.every((indexSet) => current[indexSet.id] === indexSet)
        ? current
        : { ...current, ...Object.fromEntries(data.list.map((indexSet) => [indexSet.id, indexSet])) },
    );
  const onProfileChangeApplied = (response: ProfileChangeResponse) => {
    const applied = new Set(appliedIndexSetIds(response));

    setProfileChange(
      (current) => current && { ...current, indexSets: current.indexSets.filter(({ id }) => !applied.has(id)) },
    );
    profileChange?.onChangeApplied(response);
  };

  return (
    <>
      <PaginatedEntityTable<IndexSetEntity>
        humanName="index sets"
        searchPlaceholder="Find index sets"
        additionalAttributes={additionalAttributes}
        entityActions={renderActions}
        tableLayout={activeLayout}
        fetchEntities={fetchIndexSets}
        onDataLoaded={onDataLoaded}
        keyFn={keyFn}
        bulkSelection={{
          actions: <BulkActions indexSetsById={indexSetsById} onProfileAction={setProfileChange} />,
        }}
        expandedSectionRenderers={expandedSections}
        entityAttributesAreCamelCase={false}
        columnRenderers={customColumnRenderers(extensionColumnRenderers)}
        topSection={TopSection}
      />
      {profileChange && (
        <SetProfileModal
          show
          action={profileChange.action}
          indexSets={profileChange.indexSets}
          onClose={() => setProfileChange(null)}
          onChangeApplied={onProfileChangeApplied}
        />
      )}
    </>
  );
};

export default IndexSetsOverview;
