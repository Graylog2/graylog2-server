import Immutable from 'immutable';
import uuid from 'uuid/v4';

import { widgetDefinition } from 'enterprise/logic/Widget';
import { searchTypeDefinition } from 'enterprise/logic/SearchType';

export default class SearchRequest {
  constructor(queries, widgets) {
    this.widgetMapping = new Immutable.Map();
    this.searchRequest = {
      queries: queries.map((query, id) => {
        const searchTypes = widgets.get(id, new Immutable.Map())
          .map(widget => widgetDefinition(widget.get('type')).searchTypes(widget.get('config')).map(searchType => Object.assign(searchType, { widgetId: widget.get('id') })))
          .reduce((acc, cur) => acc.merge(cur), Immutable.Set());
        const transformed = {
          id: id,
          // TODO create conversion objects for query objects
          query: {
            type: 'elasticsearch',
            query_string: query.get('query'),
          },
          // TODO create conversion objects for timerange objects
          timerange: {
            type: query.get('rangeType'),
            range: query.getIn(['rangeParams', 'range']),
          },
          // TODO the view state should reflect what search types we will be requesting for each query
          search_types: searchTypes.map((searchType) => {
            const searchTypeId = uuid();
            this.widgetMapping = this.widgetMapping.update(searchType.widgetId, new Immutable.Set(), widgetSearchTypes => widgetSearchTypes.add(searchTypeId));
            return new Immutable.Map(searchTypeDefinition(searchType.type).defaults)
              .merge(searchType.config)
              .merge(
                {
                  id: searchTypeId,
                  type: searchType.type,
                });
          }),
        };

        // console.log(query, transformed);
        return transformed;
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