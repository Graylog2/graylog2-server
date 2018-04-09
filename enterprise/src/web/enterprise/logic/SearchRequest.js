import Immutable from 'immutable';
import uuid from 'uuid/v4';

import { widgetDefinition } from 'enterprise/logic/Widget';
import { searchTypeDefinition } from 'enterprise/logic/SearchType';

const _streamFilters = (selectedStreams) => {
  return selectedStreams.map(stream => ({ type: 'stream', id: stream }));
};

const _filtersForQuery = (filters) => {
  const streamFilters = _streamFilters(filters.get('streams', []));
  if (streamFilters.length === 0) {
    return null;
  }

  return {
    type: 'or',
    filters: streamFilters,
  };
};

export default class SearchRequest {
  constructor(queries, widgets, filters) {
    this.id = uuid();
    this.widgetMapping = new Immutable.Map();
    this.searchRequest = {
      queries: queries.map((query, id) => {
        const searchTypes = widgets.get(id, new Immutable.Map())
          .map(widget => widgetDefinition(widget.get('type')).searchTypes(widget.get('config')).map(searchType => Object.assign(searchType, { widgetId: widget.get('id') })))
          .reduce((acc, cur) => acc.merge(cur), Immutable.Set());
        const filter = _filtersForQuery(filters.get(id, new Immutable.Map()));
        return {
          id: id,
          // TODO create conversion objects for query objects
          query: {
            type: 'elasticsearch',
            query_string: query.get('query'),
          },
          filter,
          // TODO create conversion objects for timerange objects
          timerange: {
            type: query.get('rangeType'),
            range: query.getIn(['rangeParams', 'range']),
          },
          search_types: searchTypes.map((searchType) => {
            const searchTypeId = uuid();
            this.widgetMapping = this.widgetMapping.update(searchType.widgetId, new Immutable.Set(), widgetSearchTypes => widgetSearchTypes.add(searchTypeId));
            const typeDefinition = searchTypeDefinition(searchType.type);
            if (!typeDefinition || !typeDefinition.defaults) {
              console.warn(`Unable to find type definition or defaults for search type ${searchType.type} - skipping!`);
            }
            const defaults = typeDefinition ? typeDefinition.defaults : {};
            return new Immutable.Map(defaults)
              .merge(searchType.config)
              .merge(
                {
                  id: searchTypeId,
                  type: searchType.type,
                });
          })
            .add({
              id: uuid(),
              type: 'messages',
              limit: 150,
              offset: 0,
            })
            .toJS(),
        };
      }).valueSeq().toJS(),
    };
  }

  toRequest() {
    return this.searchRequest;
  }

  getWidgetMapping() {
    return this.widgetMapping;
  }
}