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
          type: 'createWidget',
          description: 'Creates widget with specified parameters',
          action: () => {},
        },
        {
          type: 'updateSearchQuery',
          description:
            'Updates current search query. Parameter should be object with single string `query` key, containing the new query in correct lucene query language syntax.',
          action: (query: string) =>
            dispatch((_dispatch, getState) => {
              const activeQuery = selectActiveQuery(getState());

              return _dispatch(updateQueryString(activeQuery, query));
            }),
        },
      ],
    }),
    [dispatch, currentQuery, fieldTypes],
  );

  usePageContext(context);

  return null;
};

export default SearchPageContext;
