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
import React, { useMemo, useCallback } from 'react';
import styled from 'styled-components';

import { PaginatedEntityTable } from 'components/common';
import { DEFAULT_LAYOUT } from 'views/logic/fieldactions/ChangeFieldType/Constants';
import type { SearchParams } from 'stores/PaginationTypes';
import type { SortableAttrbutes } from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypeUsages';
import { queryKey, fetchFieldTypeUsages } from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypeUsages';
import type { FieldTypeUsage, FieldTypes } from 'views/logic/fieldactions/ChangeFieldType/types';
import useColumnRenderers from 'views/logic/fieldactions/ChangeFieldType/hooks/useColumnRenderers';
import BulkActionsDropdown from 'components/common/EntityDataTable/BulkActionsDropdown';
import useCurrentStream from 'views/logic/fieldactions/ChangeFieldType/hooks/useCurrentStream';
import isIndexFieldTypeChangeAllowed from 'components/indices/helpers/isIndexFieldTypeChangeAllowed';
import useIndexSetsList from 'components/indices/hooks/useIndexSetsList';
import type { IndexSet } from 'stores/indices/IndexSetsStore';

const Container = styled.div`
  margin-top: 20px;
`;

type Props = {
  field: string;
  setIndexSetSelection: React.Dispatch<Array<string>>;
  fieldTypes: FieldTypes;
  initialSelection: Array<string>;
};

const mapper = (indexSets: Array<IndexSet>): Record<string, IndexSet> => {
  if (!indexSets) return null;

  return Object.fromEntries(indexSets.map((indexSet) => [indexSet.id, indexSet]));
};

const IndexSetsTable = ({ field, setIndexSetSelection, fieldTypes, initialSelection }: Props) => {
  const { data } = useIndexSetsList();
  const currentStreams = useCurrentStream();

  const mappedIndexSets = useMemo(() => mapper(data?.indexSets), [data?.indexSets]);

  const fetchEntities = (searchParams: SearchParams<SortableAttrbutes>) =>
    fetchFieldTypeUsages({ field, streams: currentStreams }, searchParams);
  const columnRenderers = useColumnRenderers(fieldTypes);

  const onChangeSelection = useCallback(
    (newSelection: Array<string>) => {
      setIndexSetSelection(newSelection);
    },
    [setIndexSetSelection],
  );

  const isEntitySelectable = useCallback(
    (entity) => {
      const indexSetId = entity.id;

      return isIndexFieldTypeChangeAllowed(mappedIndexSets[indexSetId]);
    },
    [mappedIndexSets],
  );

  return (
    <Container>
      <PaginatedEntityTable<FieldTypeUsage>
        humanName="Index Sets"
        tableLayout={DEFAULT_LAYOUT}
        withoutURLParams
        fetchEntities={fetchEntities}
        keyFn={(searchParams) => queryKey(searchParams, field, currentStreams)}
        columnRenderers={columnRenderers}
        entityAttributesAreCamelCase
        bulkSelection={{
          onChangeSelection,
          initialSelection,
          actions: <BulkActionsDropdown />,
          isEntitySelectable,
        }}
        searchPlaceholder="Search for index sets"
        fetchOptions={{ refetchInterval: 5000 }}
      />
    </Container>
  );
};

export default IndexSetsTable;
