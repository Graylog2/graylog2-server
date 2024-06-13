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
import React, { useCallback, useMemo, useState } from 'react';
import styled, { css } from 'styled-components';
import pickBy from 'lodash/pickBy';
import keyBy from 'lodash/keyBy';

import { fetchIndexSetFieldTypes, keyFn } from 'components/indices/IndexSetFieldTypes/hooks/useIndexSetFieldType';
import useParams from 'routing/useParams';
import { Icon, PaginatedEntityTable } from 'components/common';
import type { Sort } from 'stores/PaginationTypes';
import FieldTypeActions from 'components/indices/IndexSetFieldTypes/FieldTypeActions';
import expandedSections from 'components/indices/IndexSetFieldTypes/originExpandedSections/expandedSections';
import type { FieldTypeOrigin, IndexSetFieldType } from 'components/indices/IndexSetFieldTypes/types';
import OriginFilterValueRenderer from 'components/indices/IndexSetFieldTypes/OriginFilterValueRenderer';
import useCustomColumnRenderers from 'components/indices/IndexSetFieldTypes/hooks/useCustomColumnRenderers';
import IndexSetProfile from 'components/indices/IndexSetFieldTypes/IndexSetProfile';
import type { FieldTypePutResponse } from 'views/logic/fieldactions/ChangeFieldType/types';
import { useStore } from 'stores/connect';
import { IndexSetsStore } from 'stores/indices/IndexSetsStore';
import isIndexFieldTypeChangeAllowed from 'components/indices/helpers/isIndexFieldTypeChangeAllowed';

import BulkActions from './BulkActions';

export const DEFAULT_LAYOUT = {
  entityTableId: 'index-set-field-types',
  defaultPageSize: 20,
  defaultSort: { attributeId: 'field_name', direction: 'asc' } as Sort,
  defaultDisplayedAttributes: ['field_name', 'type', 'origin', 'is_reserved'],
};

const COLUMNS_ORDER = ['field_name', 'type', 'origin', 'is_reserved'];

const StyledIcon = styled(Icon)<{ $value: 'true' | 'false' }>(({ theme, $value }) => css`
  color: ${$value === 'true' ? theme.colors.variant.success : theme.colors.variant.danger};
  margin-right: 5px;
`);
const isEntitySelectable = (fieldType: IndexSetFieldType) => !fieldType.isReserved;
const FilterValueRenderers = {
  is_reserved: (value: 'true' | 'false', title: string) => (
    <>
      <StyledIcon name={value === 'true' ? 'check_circle' : 'cancel'} $value={value} />
      {title}
    </>
  ),
  origin: (value: FieldTypeOrigin, title: string) => <OriginFilterValueRenderer title={title} origin={value} />,
};

const IndexSetFieldTypesList = () => {
  const { indexSetId } = useParams();
  const { indexSet } = useStore(IndexSetsStore);
  const [selectedEntitiesData, setSelectedEntitiesData] = useState<Record<string, IndexSetFieldType>>({});
  const customColumnRenderers = useCustomColumnRenderers();

  const onSubmitCallback = useCallback((response: FieldTypePutResponse, refetchFieldTypes: () => void) => {
    const newEntityFieldName = response?.[indexSetId]?.fieldName;

    if (newEntityFieldName && selectedEntitiesData[newEntityFieldName]) {
      setSelectedEntitiesData({ ...selectedEntitiesData, [newEntityFieldName]: response[indexSetId] });
    }

    refetchFieldTypes();
  }, [indexSetId, selectedEntitiesData]);
  const indexFieldTypeChangeAllowed = useMemo(() => isIndexFieldTypeChangeAllowed(indexSet), [indexSet]);
  const renderActions = useCallback((fieldType: IndexSetFieldType) => (
    <FieldTypeActions fieldType={fieldType}
                      indexSetId={indexSetId}
                      onSubmitCallback={onSubmitCallback} />
  ), [indexSetId, onSubmitCallback]);

  const bulkSelection = useMemo(() => ({
    onChangeSelection: (selectedItemsIds: Array<string>, list: Array<IndexSetFieldType>) => {
      setSelectedEntitiesData((cur) => {
        const selectedItemsIdsSet = new Set(selectedItemsIds);
        const filtratedCurrentItems = pickBy(cur, (_, fieldName) => selectedItemsIdsSet.has(fieldName));
        const filtratedCurrentEntries = list.filter(({ fieldName }) => selectedItemsIdsSet.has(fieldName));
        const listOfCurrentEntries = keyBy(filtratedCurrentEntries, 'id');

        return ({ ...filtratedCurrentItems, ...listOfCurrentEntries });
      });
    },
    actions: <BulkActions indexSetId={indexSetId} selectedEntitiesData={selectedEntitiesData} />,
    isEntitySelectable,
  }), [indexSetId, selectedEntitiesData]);

  return (
    <PaginatedEntityTable<IndexSetFieldType> humanName="index set field types"
                                             columnsOrder={COLUMNS_ORDER}
                                             entityActions={indexFieldTypeChangeAllowed && renderActions}
                                             tableLayout={DEFAULT_LAYOUT}
                                             topRightCol={indexFieldTypeChangeAllowed && <IndexSetProfile />}
                                             fetchEntities={(searchParams) => fetchIndexSetFieldTypes(indexSetId, searchParams)}
                                             keyFn={keyFn}
                                             bulkSelection={bulkSelection}
                                             expandedSectionsRenderer={expandedSections}
                                             entityAttributesAreCamelCase
                                             filterValueRenderers={FilterValueRenderers}
                                             columnRenderers={customColumnRenderers} />
  );
};

export default IndexSetFieldTypesList;
