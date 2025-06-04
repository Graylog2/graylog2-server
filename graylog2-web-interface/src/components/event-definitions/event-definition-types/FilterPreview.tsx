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
import { useEffect, useMemo, useState } from 'react';
import * as Immutable from 'immutable';
import debounce from 'lodash/debounce';

import { Table } from 'components/bootstrap';
import { Spinner } from 'components/common';
import HelpPanel from 'components/event-definitions/common/HelpPanel';
import type { EventDefinition } from 'components/event-definitions/event-definitions-types';
import useCurrentUser from 'hooks/useCurrentUser';
import generateId from 'logic/generateId';
import type { SearchExecutionResult } from 'views/types';
import useSearchExecutors from 'views/components/contexts/useSearchExecutors';
import Search from 'views/logic/search/Search';
import createSearch from 'views/logic/slices/createSearch';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import Query from 'views/logic/queries/Query';
import type { LookupTableParameterJsonEmbryonic } from 'components/event-definitions/event-definition-types/FilterForm';
import LookupTableParameter from 'views/logic/parameters/LookupTableParameter';
import type User from 'logic/users/User';
import { isPermitted } from 'util/PermissionsMixin';
import FilterPreviewResults from 'components/event-definitions/event-definition-types/FilterPreviewResults';

type FilterPreviewProps = {
  config: EventDefinition['config'];
};

type Message = {
  index: string;
  message: {
    timestamp: string;
    _id: string;
    message: string;
  };
};

const Messages = ({ messages }: { messages: Array<Message> }) =>
  messages.map(({ index, message }) => (
    <tr key={`${index}-${message._id}`}>
      <td>{message.timestamp}</td>
      <td>{message.message}</td>
    </tr>
  ));

const SearchResult = ({
  searchResult,
  isFetchingData,
}: {
  isFetchingData: boolean;
  searchResult: { messages?: Array<Message> };
}) => {
  if (isFetchingData) return <Spinner text="Loading filter preview..." />;

  if (!searchResult.messages || searchResult.messages.length === 0) {
    return <p>Could not find any messages with the current search criteria.</p>;
  }

  return (
    <Table striped condensed bordered>
      <thead>
        <tr>
          <th>Timestamp</th>
          <th>Message</th>
        </tr>
      </thead>
      <tbody>
        <Messages messages={searchResult.messages} />
      </tbody>
    </Table>
  );
};

const constructSearch = (config: EventDefinition['config'], searchTypeId: string, queryId: string) => {
  const formattedStreams = config?.streams?.map((stream) => ({ type: 'stream', id: stream })) || [];

  const queryBuilder = Query.builder()
    .id(queryId)
    .query({ type: 'elasticsearch', query_string: config?.query || '*' })
    .timerange({ type: 'relative', range: (config?.search_within_ms || 0) / 1000 })
    .filter(formattedStreams.length === 0 ? null : Immutable.Map({ type: 'or', filters: formattedStreams }))
    .filters(config.filters)
    .searchTypes([
      {
        id: searchTypeId,
        type: 'messages',
        limit: 10,
        offset: 0,
        filter: undefined,
        filters: undefined,
        name: undefined,
        query: undefined,
        timerange: undefined,
        streams: [],
        stream_categories: [],
        sort: [],
        decorators: [],
      },
    ]);

  const query = queryBuilder.build();

  return Search.create()
    .toBuilder()
    .parameters(
      config?.query_parameters
        ?.filter((param: LookupTableParameterJsonEmbryonic) => !param.embryonic)
        .map((param) => LookupTableParameter.fromJSON(param)) ?? [],
    )
    .queries([query])
    .build();
};

const isPermittedToSeePreview = (currentUser: User, config: EventDefinition['config']) => {
  const missingPermissions = config?.streams?.some(
    (stream) => !isPermitted(currentUser.permissions, `streams:read:${stream}`),
  );

  return !missingPermissions;
};

const useExecutePreview = (config: EventDefinition['config']) => {
  const currentUser = useCurrentUser();
  const queryId = useMemo(() => generateId(), []);
  const searchTypeId = useMemo(() => generateId(), []);
  const [results, setResults] = useState<SearchExecutionResult>();
  const { startJob, executeJobResult } = useSearchExecutors();
  const executeSearch = useMemo(
    () =>
      debounce(
        (search: Search) =>
          createSearch(search)
            .then((createdSearch) => startJob(createdSearch, [searchTypeId], SearchExecutionState.empty()))
            .then((jobIds) => executeJobResult({ jobIds }))
            .then((result) => setResults(result)),
        250,
      ),
    [executeJobResult, searchTypeId, startJob],
  );

  useEffect(() => {
    if (isPermittedToSeePreview(currentUser, config)) {
      const search = constructSearch(config, searchTypeId, queryId);
      executeSearch(search);
    }
  }, [config, currentUser, executeSearch, queryId, searchTypeId]);

  return {
    errors: results?.result?.errors,
    result: results?.result?.forId(queryId)?.searchTypes?.[searchTypeId],
  };
};

const FilterPreview = ({ config }: FilterPreviewProps) => {
  const currentUser = useCurrentUser();
  const displayPreview = isPermittedToSeePreview(currentUser, config);
  const results = useExecutePreview(config);
  const { result: searchResult = {}, errors = [] } = results ?? {};
  const isFetchingData = !results?.result;
  const hasError = errors?.length > 0;

  return (
    <>
      <HelpPanel
        collapsible
        defaultExpanded={!displayPreview}
        title="How many Events will Filter & Aggregation create?">
        <p>
          The Filter & Aggregation Condition will generate different number of Events, depending on how it is
          configured:
        </p>
        <ul>
          <li>
            <b>Filter:</b>&emsp;One Event per message matching the filter
          </li>
          <li>
            <b>Aggregation without groups:</b>&emsp;One Event every time the aggregation result satisfies the condition
          </li>
          <li>
            <b>Aggregation with groups:</b>&emsp;One Event per group whose aggregation result satisfies the condition
          </li>
        </ul>
      </HelpPanel>

      {displayPreview && (
        <FilterPreviewResults hasError={hasError}>
          {hasError ? (
            <p>{errors[0].description}</p>
          ) : (
            <SearchResult isFetchingData={isFetchingData} searchResult={searchResult} />
          )}
        </FilterPreviewResults>
      )}
    </>
  );
};

export default FilterPreview;
