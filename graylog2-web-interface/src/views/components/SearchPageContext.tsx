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
import { useMemo } from 'react';

import { updateQueryString } from 'views/logic/slices/viewSlice';
import useViewsDispatch from 'views/stores/useViewsDispatch';
import { selectActiveQuery } from 'views/logic/slices/viewSelectors';
import usePageContext from 'hooks/usePageContext';
import useCurrentQuery from 'views/logic/queries/useCurrentQuery';
import useFieldTypes from 'views/logic/fieldtypes/useFieldTypes';
import { filtersToStreamSet } from 'views/logic/queries/Query';

const SearchPageContext = () => {
  const dispatch = useViewsDispatch();
  const currentQuery = useCurrentQuery();
  const currentStreams = useMemo(() => filtersToStreamSet(currentQuery?.filter).toArray(), [currentQuery?.filter]);
  const { data: fieldTypesData } = useFieldTypes(currentStreams, currentQuery?.timerange);
  const fieldTypes = useMemo(
    () =>
      Object.fromEntries(
        fieldTypesData?.map((fieldType) => [
          fieldType.name,
          { type: fieldType.type.type, properties: fieldType.type.properties.toArray() },
        ]) ?? [],
      ),
    [fieldTypesData],
  );
  const context = useMemo(
    () => ({
      type: 'search',
      additional: { currentQuery, fields: fieldTypes },
      actions: [
        {
          name: 'createWidget',
          description: 'Creates widget with specified parameters',
          action: console.log,
          parameters_schema: {
            type: 'string',
            query_string: 'string',
            streams: 'array',
          },
        },
        {
          name: 'updateSearchQuery',
          description:
            'Updates current search query. Suggested parameter should contain a valid query in correct lucene query language syntax.',
          action: ({ query }: { query: string }) =>
            dispatch((_dispatch, getState) => {
              const activeQuery = selectActiveQuery(getState());

              return _dispatch(updateQueryString(activeQuery, query));
            }),
          parameters_schema: {
            query: 'string',
          },
        },
      ],
    }),
    [dispatch, currentQuery, fieldTypes],
  );

  usePageContext(context);

  return null;
};

export default SearchPageContext;
