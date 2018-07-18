import { mapValues } from 'lodash';

import { searchTypeDefinition } from 'enterprise/logic/SearchType';

const _findMessages = (results) => {
  return Object.keys(results.searchTypes)
    .map(id => results.searchTypes[id])
    .find(searchType => searchType.type.toLocaleLowerCase() === 'messages');
};

const _searchTypePlugin = (type) => {
  const typeDefinition = searchTypeDefinition(type);
  return typeDefinition && typeDefinition.handler ? searchTypeDefinition(type).handler :
    {
      convert: (result) => {
        console.log(`No search type handler for type '${type}' result:`, result);
        return result;
      },
    };
};

export default class QueryResult {
  constructor(queryResult) {
    // eslint-disable-next-line camelcase
    const { duration, timestamp, effective_timerange } = queryResult.execution_stats;
    this._state = {
      query: queryResult.query,
      duration,
      timestamp,
      effectiveTimerange: effective_timerange,
      searchTypes: mapValues(queryResult.search_types, (searchType) => {
        // each search type has a custom data structure attached to it, let the plugin convert the value
        return _searchTypePlugin(searchType.type).convert(searchType);
      }),
    };
  }

  get query() { return this._state.query; }
  get duration() { return this._state.duration; }
  get timestamp() { return this._state.timestamp; }
  get effectiveTimerange() { return this._state.effectiveTimerange; }
  get searchTypes() { return this._state.searchTypes; }
  get documentCount() {
    const messages = _findMessages(this);
    return messages.total;
  }
  get messages() {
    return _findMessages(this);
  }
}

