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
import { useCallback, useEffect, useMemo, useState } from 'react';
import styled from 'styled-components';

import { Row, Col } from 'components/bootstrap';
import { IfPermitted, Spinner, SearchForm } from 'components/common';
import { naturalSortIgnoreCase } from 'util/SortUtils';
import EntityList from 'components/common/EntityList';
import { InputsActions, InputsStore } from 'stores/inputs/InputsStore';
import { InputTypesActions } from 'stores/inputs/InputTypesStore';
import { SingleNodeActions, SingleNodeStore } from 'stores/nodes/SingleNodeStore';
import { useStore } from 'stores/connect';
import type { StoreState } from 'stores/StoreTypes';
import type { NodeInfo } from 'stores/nodes/NodesStore';
import type { Input } from 'components/messageloaders/Types';

import InputListItem from './InputListItem';
import CreateInputControl from './CreateInputControl';

const InputListRow = styled(Row)`
  h2 {
    margin-bottom: 5px;
  }

  .alert {
    margin-top: 10px;
  }

  .static-fields {
    margin-top: 10px;
    margin-left: 3px;

    ul {
      margin: 0;
      padding: 0;

      .remove-static-field {
        margin-left: 5px;
      }
    }
  }
`;

const _splitInputs = (state: StoreState<typeof InputsStore>, node: NodeInfo) => {
  const { inputs } = state ?? {};
  const globalInputs = inputs?.filter((input) => input.global === true)
    .sort((inputA, inputB) => naturalSortIgnoreCase(inputA.title, inputB.title));
  let localInputs = inputs?.filter((input) => input.global === false)
    .sort((inputA, inputB) => naturalSortIgnoreCase(inputA.title, inputB.title));

  if (node?.node_id) {
    localInputs = localInputs?.filter((input) => input.node === node.node_id);
  }

  return {
    globalInputs,
    localInputs,
  };
};

const _onFilterInputs = (globalInputs: Array<Input>, localInputs: Array<Input>, filter: string) => {
  const regExp = RegExp(filter, 'i');
  const filterMethod = (input: Input) => regExp.test(input.title);

  return ((!globalInputs || !localInputs) || (!filter || filter.length <= 0))
    ? {
      filteredGlobalInputs: globalInputs,
      filteredLocalInputs: localInputs,
    }
    : {
      filteredGlobalInputs: globalInputs.filter(filterMethod),
      filteredLocalInputs: localInputs.filter(filterMethod),
    };
};

type Props = {
  permissions: Array<string>,
  node: NodeInfo,
}

const InputsList = ({ permissions, node }: Props) => {
  useEffect(() => {
    InputTypesActions.list();
    InputsActions.list();
    SingleNodeActions.get();
  }, []);

  const currentNode = useStore(SingleNodeStore);
  const { globalInputs, localInputs } = useStore(InputsStore, (inputsStore) => _splitInputs(inputsStore, node));
  const [filter, setFilter] = useState<string>();
  const resetFilter = useCallback(() => setFilter(undefined), []);
  const { filteredGlobalInputs, filteredLocalInputs } = useMemo(
    () => _onFilterInputs(globalInputs, localInputs, filter),
    [filter, globalInputs, localInputs]);

  const nodeAffix = node ? ' on this node' : '';

  if (!(localInputs && globalInputs && currentNode && filteredLocalInputs && filteredGlobalInputs)) {
    return <Spinner />;
  }

  return (
    <div>
      {!node && (
      <IfPermitted permissions="inputs:create">
        <CreateInputControl />
      </IfPermitted>
      )}

      <InputListRow id="filter-input" className="content">
        <Col md={12}>
          <SearchForm onSearch={setFilter}
                      topMargin={0}
                      onReset={resetFilter}
                      placeholder="Filter by title" />
          <br />
          <h2>
            Global inputs
            &nbsp;
            <small>{globalInputs.length} configured{nodeAffix}</small>
          </h2>
          <EntityList bsNoItemsStyle="info"
                      noItemsText={globalInputs.length <= 0 ? 'There are no global inputs.'
                        : 'No global inputs match the filter'}
                      items={filteredGlobalInputs.map((input) => (
                        <InputListItem key={input.id}
                                       input={input}
                                       currentNode={currentNode}
                                       permissions={permissions} />
                      ))} />
          <br />
          <br />
          <h2>
            Local inputs
            &nbsp;
            <small>{localInputs.length} configured{nodeAffix}</small>
          </h2>
          <EntityList bsNoItemsStyle="info"
                      noItemsText={localInputs.length <= 0 ? 'There are no local inputs.'
                        : 'No local inputs match the filter'}
                      items={filteredLocalInputs.map((input) => (
                        <InputListItem key={input.id}
                                       input={input}
                                       currentNode={currentNode}
                                       permissions={permissions} />
                      ))} />
        </Col>
      </InputListRow>
    </div>
  );
};

export default InputsList;
