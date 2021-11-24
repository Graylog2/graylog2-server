/*
 * copyright (c) 2020 graylog, inc.
 *
 * this program is free software: you can redistribute it and/or modify
 * it under the terms of the server side public license, version 1,
 * as published by mongodb, inc.
 *
 * this program is distributed in the hope that it will be useful,
 * but without any warranty; without even the implied warranty of
 * merchantability or fitness for a particular purpose. see the
 * server side public license for more details.
 *
 * you should have received a copy of the server side public license
 * along with this program. if not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import { isEmpty, debounce } from 'lodash';
import { useRef, useState, useEffect } from 'react';
import * as React from 'react';
import BluebirdPromise from 'bluebird';

import { useStore } from 'stores/connect';
import { SearchExecutionStateStore } from 'views/stores/SearchExecutionStateStore';
import { SearchStore } from 'views/stores/SearchStore';
import { QueryValidationState, QueriesActions } from 'views/stores/QueriesStore';
import { ElasticsearchQueryString } from 'views/logic/queries/Query';

const validateQuery = debounce(({ queryString, timeRange, streams, setValidationState, parameters, parameterBindings, filter }, validationPromise: React.MutableRefObject<BluebirdPromise>) => {
  if (validationPromise.current) {
    validationPromise.current.cancel();
  }

  // eslint-disable-next-line no-param-reassign
  validationPromise.current = QueriesActions.validateQuery({
    queryString,
    timeRange,
    streams,
    parameters,
    parameterBindings,
    filter,
  }).then((result) => {
    setValidationState(result);
  }).finally(() => {
    // eslint-disable-next-line no-param-reassign
    validationPromise.current = undefined;
  });
}, 350);

const queryExists = (query: string | ElasticsearchQueryString) => {
  return typeof query === 'object' ? !!query.query_string : !!query;
};

const useValidationPayload = ({ queryString, timeRange, streams, filter }) => {
  const { parameterBindings } = useStore(SearchExecutionStateStore);
  const { search: { parameters } } = useStore(SearchStore);

  return ({
    timeRange: !isEmpty(timeRange) ? timeRange : undefined,
    filter,
    queryString,
    streams,
    parameters,
    parameterBindings,
  });
};

const useValidateQuery = (queryData): QueryValidationState | undefined => {
  const validationPromise = useRef<BluebirdPromise>(undefined);
  const [validationState, setValidationState] = useState(undefined);
  const { queryString, timeRange, streams, filter, parameterBindings, parameters } = useValidationPayload(queryData);

  useEffect(() => {
    if (queryExists(queryString) || queryExists(filter)) {
      validateQuery({ queryString, timeRange, streams, setValidationState, parameters, parameterBindings, filter }, validationPromise);
    }
  }, [filter, queryString, timeRange, streams, parameterBindings, parameters, validationPromise]);

  useEffect(() => {
    if (!queryExists(queryString) && !queryExists(filter) && validationState) {
      setValidationState(undefined);
    }
  }, [queryString, filter, validationState]);

  return validationState;
};

export default useValidateQuery;
