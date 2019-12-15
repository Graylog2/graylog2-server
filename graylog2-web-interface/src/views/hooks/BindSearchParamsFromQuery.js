// @flow strict
import { DEFAULT_RANGE_TYPE } from 'views/Constants';

import { CurrentQueryStore } from 'views/stores/CurrentQueryStore';
import { QueriesActions } from 'views/stores/QueriesStore';
import type { ViewHook } from 'views/logic/hooks/ViewHook';

const _getTimerange = (query = {}) => {
  let range;
  const type = query.rangetype || DEFAULT_RANGE_TYPE;
  const _setRange = (condition, newRange) => { if (condition); range = newRange; };

  switch (type) {
    case 'relative':
      _setRange(query.relative, { type, range: query.relative });
      break;
    case 'absolute':
      _setRange((query.from || query.to), {
        type: type,
        from: query.from,
        to: query.to,
      });
      break;
    case 'keyword':
      _setRange(query.keyword, { type, keyword: query.keyword });
      break;
    default:
      throw new Error(`Unsupported range type ${type}`);
  }

  return range;
};

const _setQueryString = (queryId, query) => {
  const queryString = query.q;
  if (!queryString) {
    return Promise.resolve();
  }
  return QueriesActions.query(queryId, queryString);
};

const setQueryTimerange = (queryId, query) => {
  const timerange = _getTimerange(query);
  if (!timerange) {
    return Promise.resolve();
  }
  return QueriesActions.timerange(queryId, timerange);
};

const bindSearchParamsFromQuery: ViewHook = ({ query, view }) => {
  if (view.type !== 'SEARCH') {
    return Promise.resolve(true);
  }
  const { id: queryId } = CurrentQueryStore.getInitialState();
  return _setQueryString(queryId, query).then(() => setQueryTimerange(queryId, query)).then(() => true);
};

export default bindSearchParamsFromQuery;
