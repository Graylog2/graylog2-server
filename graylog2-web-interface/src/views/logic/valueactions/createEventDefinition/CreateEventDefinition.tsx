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
import React, { useContext } from 'react';

import type { ActionComponentProps } from 'views/components/actions/ActionHandler';
import CreateEventDefinitionModal from 'views/logic/valueactions/createEventDefinition/CreateEventDefinitionModal';
import useMappedData from 'views/logic/valueactions/createEventDefinition/hooks/useMappedData';
import useModalData from 'views/logic/valueactions/createEventDefinition/hooks/useModalData';
import { ActionContext } from 'views/logic/ActionContext';

const concatQuery = (queryParts: Array<string>) => {
  return queryParts.reduce((res, queryPart) => {
    let curRes = res;

    if (queryPart) {
      curRes = `${res}${res ? ' AND ' : ' '}(${queryPart})`;
    }

    return curRes;
  }, '');
};

const CreateEventDefinition = ({
  value,
  field,
  queryId,
}: ActionComponentProps) => {
  const contexts = useContext(ActionContext);
  /*
  const eventDefinitionUrlConfig = ({
    query: concatQuery([queryWithReplacedParams, searchFilterQuery, searchFromAggregation, columnValuePath, rowValuePath]),
    loc_query_parameters: lutParameters,
    search_within_ms,
    type: 'aggregation-v1',
    streams,
    columnGroupBy,
    rowGroupBy,
    columnValuePath,
    rowValuePath,
    ...aggregationVales,
  });
  */

  const mappedData = useMappedData({ contexts, field, queryId, value });
  const modalData = useModalData(mappedData);

  return <CreateEventDefinitionModal modalData={modalData} />;
};

export default CreateEventDefinition;
