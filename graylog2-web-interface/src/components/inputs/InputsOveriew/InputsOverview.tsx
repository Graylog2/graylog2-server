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
import { useCallback, useMemo } from 'react';
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
import { IfPermitted } from 'components/common';
import type { SearchParams } from 'stores/PaginationTypes';

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
  const resolvedKeyFn = useCallback(
    (searchParams: SearchParams) => [...KEY_PREFIX, entityTableId ?? tableLayout.entityTableId, searchParams],
    [entityTableId, tableLayout.entityTableId],
  );
  const { entityActions, expandedSections } = useTableElements({
    inputTypes,
    inputTypeDescriptions,
  });
  const columnRenderers = useMemo(() => customColumnRenderers({ inputTypes, inputStates }), [inputTypes, inputStates]);
  const fetchEntities = (options: SearchParams) => {
    const optionsCopy = { ...options };

    if (node) {
      optionsCopy.filters = Immutable.OrderedMap(options.filters).set('node_id', [node.node_id]);
    } else if (global) {
      optionsCopy.filters = Immutable.OrderedMap(options.filters).set('global', ['true']);
    }

    return fetchInputs(optionsCopy);
  };

  return (
    <div>
      {!node && !global && (
        <IfPermitted permissions="inputs:create">
          <CreateInputControl />
        </IfPermitted>
      )}
      <PaginatedEntityTable<Input>
        humanName="inputs"
        additionalAttributes={additionalAttributes}
        queryHelpComponent={<QueryHelper entityName={entityName} />}
        entityActions={entityActions}
        tableLayout={resolvedTableLayout}
        fetchEntities={fetchEntities}
        expandedSectionRenderers={expandedSections}
        keyFn={resolvedKeyFn}
        bulkSelection={undefined}
        withoutURLParams={withoutURLParams}
        entityAttributesAreCamelCase={false}
        filterValueRenderers={{}}
        columnRenderers={columnRenderers}
      />
    </div>
  );
};

export default InputsOverview;
