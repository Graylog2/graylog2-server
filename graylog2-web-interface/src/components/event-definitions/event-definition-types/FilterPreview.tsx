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
import { useMemo, useContext } from 'react';
import * as Immutable from 'immutable';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';

import { Table } from 'components/bootstrap';
import { Spinner } from 'components/common';
import HelpPanel from 'components/event-definitions/common/HelpPanel';
import type { EventDefinition } from 'components/event-definitions/event-definitions-types';
import generateId from 'logic/generateId';
import useSearchExecutors from 'views/components/contexts/useSearchExecutors';
import Search from 'views/logic/search/Search';
import createSearch from 'views/logic/slices/createSearch';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import Query from 'views/logic/queries/Query';
import type { LookupTableParameterJsonEmbryonic } from 'components/event-definitions/event-definition-types/FilterForm';
import Parameter from 'views/logic/parameters/Parameter';
import usePermissions from 'hooks/usePermissions';
import type { IsPermitted } from 'hooks/usePermissions';
import FilterPreviewResults from 'components/event-definitions/event-definition-types/FilterPreviewResults';
import useDebouncedValue from 'hooks/useDebouncedValue';
import StreamsContext from 'contexts/StreamsContext';
import type { Stream } from 'logic/streams/types';

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
  const { t } = useTranslation('event_definitions');

  if (isFetchingData) return <Spinner text={t('filter_preview.loading')} />;

  if (!searchResult.messages || searchResult.messages.length === 0) {
    return <p>{t('filter_preview.no_messages')}</p>;
  }

  return (
    <Table condensed bordered>
      <thead>
        <tr>
          <th>{t('filter_preview.timestamp')}</th>
          <th>{t('filter_preview.message')}</th>
        </tr>
      </thead>
      <tbody>
        <Messages messages={searchResult.messages} />
      </tbody>
    </Table>
  );
};

const constructSearch = (
  config: EventDefinition['config'],
  searchTypeId: string,
  queryId: string,
  streams: Stream[],
) => {
  const combinedStreamsAndCategories = new Set([
    ...(config?.streams || []),
    ...(config?.stream_categories?.flatMap((category) =>
      streams.filter((s) => s.categories.includes(category)).map((s) => s.id),
    ) || []),
  ]);
  const formattedStreams = [...combinedStreamsAndCategories].map((stream) => ({ type: 'stream', id: stream })) || [];

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
        .map((param) => Parameter.fromJSON(param)) ?? [],
    )
    .queries([query])
    .build();
};

const isPermittedToSeePreview = (isPermitted: IsPermitted, config: EventDefinition['config']) => {
  const missingPermissions = config?.streams?.some((stream) => !isPermitted(`streams:read:${stream}`));

  return !missingPermissions;
};

const useExecutePreview = (config: EventDefinition['config']) => {
  const streams = useContext(StreamsContext);

  const { isPermitted } = usePermissions();
  const queryId = useMemo(() => generateId(), []);
  const searchTypeId = useMemo(() => generateId(), []);
  const { startJob, executeJobResult } = useSearchExecutors();
  const permitted = isPermittedToSeePreview(isPermitted, config);
  const [debouncedConfig] = useDebouncedValue(config, 250);
  const hasEmbryonicParams = debouncedConfig?.query_parameters?.some(
    (param: LookupTableParameterJsonEmbryonic) => param.embryonic,
  );

  const { data, isFetching } = useQuery({
    queryKey: ['filter-preview', debouncedConfig, searchTypeId, queryId, streams],
    queryFn: async () => {
      const search = constructSearch(debouncedConfig, searchTypeId, queryId, streams);
      const createdSearch = await createSearch(search);
      const jobIds = await startJob(createdSearch, [searchTypeId], SearchExecutionState.empty());
      const result = await executeJobResult({ jobIds });

      return result;
    },
    enabled: permitted && !hasEmbryonicParams,
    retry: false,
  });

  return {
    isFetching,
    errors: data?.result?.errors,
    result: data?.result?.forId(queryId)?.searchTypes?.[searchTypeId],
  };
};

const FilterPreview = ({ config }: FilterPreviewProps) => {
  const { t } = useTranslation('event_definitions');
  const { isPermitted } = usePermissions();
  const displayPreview = isPermittedToSeePreview(isPermitted, config);
  const { isFetching, result: searchResult = {}, errors = [] } = useExecutePreview(config);
  const isFetchingData = isFetching || !searchResult;
  const hasError = errors?.length > 0;

  return (
    <>
      <HelpPanel collapsible defaultExpanded={!displayPreview} title={t('filter_preview.help_title')}>
        <p>{t('filter_preview.help_description')}</p>
        <ul>
          <li>
            <b>{t('filter_preview.help_filter_label')}</b>&emsp;{t('filter_preview.help_filter')}
          </li>
          <li>
            <b>{t('filter_preview.help_aggregation_label')}</b>&emsp;{t('filter_preview.help_aggregation')}
          </li>
          <li>
            <b>{t('filter_preview.help_aggregation_groups_label')}</b>&emsp;
            {t('filter_preview.help_aggregation_groups')}
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
