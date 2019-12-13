// @flow strict
import { DEFAULT_RANGE_TYPE } from 'views/Constants';

import View from 'views/logic/views/View';
import { CurrentQueryStore } from 'views/stores/CurrentQueryStore';
import { QueriesActions } from 'views/stores/QueriesStore';
import type { ViewHook } from 'views/logic/hooks/ViewHook';

type Arguments = {
  query: { [string]: any },
  view: View,
};

const _getTimerange = (query = {}) => {
  let timerange;
  const rangeType = query.rangetype || DEFAULT_RANGE_TYPE;

  switch (rangeType) {
    case 'relative':
      if (query.relative) {
        timerange = { type: rangeType, range: query.relative };
      }
      break;
    case 'absolute':
      if (query.from || query.to) {
        timerange = {
          type: rangeType,
          from: query.from,
          to: query.to,
        };
      }
      break;
    case 'keyword':
      if (query.keyword) {
        timerange = { type: rangeType, keyword: query.keyword };
      }
      break;
    default:
      throw new Error(`Unsupported range type ${rangeType}`);
  }

  return timerange;
};

const setQueryString = (queryId, queryParam) => {
  if (!queryParam) {
    return Promise.resolve();
  }
  return QueriesActions.query(queryId, queryParam);
};

const setTimerange = (queryId, timeRangeParam) => {
  if (!timeRangeParam) {
    return Promise.resolve();
  }
  return QueriesActions.timerange(queryId, timeRangeParam);
};

const bindSearchParamsFromQuery: ViewHook = ({ query, view }: Arguments) => {
  if (view.type !== 'SEARCH') {
    return Promise.resolve(true);
  }
  const { id: queryId } = CurrentQueryStore.getInitialState();
  const queryParam = query ? query.q : undefined;
  const timerangeParam = _getTimerange(query);
  return setQueryString(queryId, queryParam).then(() => setTimerange(queryId, timerangeParam)).then(() => true);
};

export default bindSearchParamsFromQuery;
