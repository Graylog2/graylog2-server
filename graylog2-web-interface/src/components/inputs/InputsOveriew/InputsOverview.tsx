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
import * as Immutable from 'immutable';

import type { NodeInfo } from 'stores/nodes/NodesStore';
import { KEY_PREFIX, fetchInputs } from 'hooks/usePaginatedInputs';
import PaginatedEntityTable from 'components/common/PaginatedEntityTable';
import QueryHelper from 'components/common/QueryHelper';
import CreateInputControl from 'components/inputs/CreateInputControl';
import customColumnRenderers from 'components/inputs/InputsOveriew/ColumnRenderers';
import getInputsTableElements from 'components/inputs/InputsOveriew/Constants';
import type { InputTypesSummary } from 'hooks/useInputTypes';
import type { InputTypeDescriptionsResponse } from 'hooks/useInputTypesDescriptions';
import useInputsStates from 'hooks/useInputsStates';
import useTableElements from 'components/inputs/InputsOveriew/useTableElements';
import { InputMetricsProvider } from 'components/inputs/InputsOveriew/InputMetricsContext';
import { backendFieldsForVisibleColumns } from 'components/inputs/InputsOveriew/metricColumns';
import useUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUserLayoutPreferences';
import { ATTRIBUTE_STATUS } from 'components/common/EntityDataTable/Constants';
import { IfPermitted } from 'components/common';
import type { SearchParams } from 'stores/PaginationTypes';
import type { PaginatedResponse } from 'components/common/PaginatedEntityTable/useFetchEntities';

type Input = {
  id: string;
  title: string;
  type: string;
  node: string;
  global: boolean;
  creator_user_id?: string;
  created_at?: string;
};

type Props = {
  node?: NodeInfo;
  global?: boolean;
  inputTypes: InputTypesSummary;
  inputTypeDescriptions: InputTypeDescriptionsResponse;
  entityTableId?: string;
  withoutURLParams?: boolean;
};

const entityName = 'input';

const InputsOverview = ({
  node = undefined,
  global = undefined,
  inputTypeDescriptions,
  inputTypes,
  entityTableId = undefined,
  withoutURLParams = false,
}: Props) => {
  const { data: inputStates } = useInputsStates();
  const { tableLayout, additionalAttributes } = getInputsTableElements();
  const resolvedTableLayout = entityTableId ? { ...tableLayout, entityTableId } : tableLayout;
  const resolvedKeyFn = (searchParams: SearchParams) => [
    ...KEY_PREFIX,
    entityTableId ?? tableLayout.entityTableId,
    searchParams,
  ];
  const { entityActions, expandedSections } = useTableElements({
    inputTypes,
    inputTypeDescriptions,
  });
  const columnRenderers = customColumnRenderers({ inputTypes, inputStates });
  const fetchEntities = (options: SearchParams) => {
    const optionsCopy = { ...options };

    if (node) {
      optionsCopy.filters = Immutable.OrderedMap(options.filters).set('node_id', [node.node_id]);
    } else if (global) {
      optionsCopy.filters = Immutable.OrderedMap(options.filters).set('global', ['true']);
    }

    return fetchInputs(optionsCopy);
  };

  const [visibleInputIds, setVisibleInputIds] = useState<Array<string>>([]);
  const onDataLoaded = (data: PaginatedResponse<Input>) => {
    const nextVisibleInputIds = data.list.map((entity) => entity.id);

    setVisibleInputIds((currentVisibleInputIds) => {
      const hasSameInputIds =
        currentVisibleInputIds.length === nextVisibleInputIds.length &&
        currentVisibleInputIds.every((inputId, index) => inputId === nextVisibleInputIds[index]);

      return hasSameInputIds ? currentVisibleInputIds : nextVisibleInputIds;
    });
  };

  const { data: layoutPreferences } = useUserLayoutPreferences(resolvedTableLayout.entityTableId);
  const userPrefs = layoutPreferences?.attributes ?? {};
  const userSelection = Object.entries(userPrefs)
    .filter(([, pref]) => pref.status === ATTRIBUTE_STATUS.show)
    .map(([attributeId]) => attributeId);
  const visibleColumns = userSelection.length > 0 ? userSelection : resolvedTableLayout.defaultDisplayedAttributes;
  const requestedFields = backendFieldsForVisibleColumns(visibleColumns);

  return (
    <div>
      {!node && !global && (
        <IfPermitted permissions="inputs:create">
          <CreateInputControl />
        </IfPermitted>
      )}
      <InputMetricsProvider inputIds={visibleInputIds} fields={requestedFields}>
        <PaginatedEntityTable<Input>
          humanName="inputs"
          additionalAttributes={additionalAttributes}
          queryHelpComponent={<QueryHelper entityName={entityName} />}
          entityActions={entityActions}
          tableLayout={resolvedTableLayout}
          fetchEntities={fetchEntities}
          onDataLoaded={onDataLoaded}
          expandedSectionRenderers={expandedSections}
          keyFn={resolvedKeyFn}
          bulkSelection={undefined}
          withoutURLParams={withoutURLParams}
          entityAttributesAreCamelCase={false}
          filterValueRenderers={{}}
          columnRenderers={columnRenderers}
        />
      </InputMetricsProvider>
    </div>
  );
};

export default InputsOverview;
