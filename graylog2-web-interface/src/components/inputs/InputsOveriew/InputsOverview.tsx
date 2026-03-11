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
import { useMemo } from 'react';
import * as Immutable from 'immutable';

import type { NodeInfo } from 'stores/nodes/NodesStore';
import { keyFn, fetchInputs } from 'hooks/usePaginatedInputs';
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
  inputTypes: InputTypesSummary;
  inputTypeDescriptions: InputTypeDescriptionsResponse;
};

const entityName = 'input';

const InputsOverview = ({ node = undefined, inputTypeDescriptions, inputTypes }: Props) => {
  const { data: inputStates } = useInputsStates();
  const { tableLayout, additionalAttributes } = getInputsTableElements();
  const { entityActions, expandedSections } = useTableElements({
    inputTypes,
    inputTypeDescriptions,
  });
  const columnRenderers = useMemo(() => customColumnRenderers({ inputTypes, inputStates }), [inputTypes, inputStates]);
  const fetchEntities = (options: SearchParams) => {
    const optionsCopy = { ...options };

    if (node) {
      optionsCopy.filters = Immutable.OrderedMap(options.filters).set('node_id', [node.node_id]);
    }

    return fetchInputs(optionsCopy);
  };

  return (
    <div>
      {!node && (
        <IfPermitted permissions="inputs:create">
          <CreateInputControl />
        </IfPermitted>
      )}
      <PaginatedEntityTable<Input>
        humanName="inputs"
        additionalAttributes={additionalAttributes}
        queryHelpComponent={<QueryHelper entityName={entityName} />}
        entityActions={entityActions}
        tableLayout={tableLayout}
        fetchEntities={fetchEntities}
        expandedSectionRenderers={expandedSections}
        keyFn={keyFn}
        bulkSelection={undefined}
        entityAttributesAreCamelCase={false}
        filterValueRenderers={{}}
        columnRenderers={columnRenderers}
      />
    </div>
  );
};

export default InputsOverview;
