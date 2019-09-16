// @flow strict
import Immutable from 'immutable';
import uuid from 'uuid/v4';
import Widget from 'views/logic/widgets/Widget';

import { widgetDefinition } from '../Widgets';
import searchTypeDefinition from '../SearchType';

const filterForWidget = widget => (widget.filter ? { filter: { type: 'query_string', query: widget.filter } } : {});

export default (widgets: Array<Widget>) => {
  let widgetMapping = Immutable.Map();
  const searchTypes = widgets
    .map(widget => widgetDefinition(widget.type)
      .searchTypes(widget.config)
      .map(searchType => Object.assign(
        searchType,
        { widgetId: widget.id, timerange: widget.timerange, query: widget.query },
        filterForWidget(widget),
      )))
    .reduce((acc, cur) => acc.merge(cur), Immutable.Set())
    .map((searchType) => {
      const searchTypeId = uuid();
      widgetMapping = widgetMapping.update(searchType.widgetId, new Immutable.Set(), widgetSearchTypes => widgetSearchTypes.add(searchTypeId));
      const typeDefinition = searchTypeDefinition(searchType.type);
      if (!typeDefinition || !typeDefinition.defaults) {
        // eslint-disable-next-line no-console
        console.warn(`Unable to find type definition or defaults for search type ${searchType.type} - skipping!`);
      }
      const defaults = typeDefinition ? typeDefinition.defaults : {};
      const { config, filter, timerange, query } = searchType;
      const filterMap = filter ? { filter } : {};
      const timerangeMap = timerange ? { timerange } : {};
      const queryMap = query ? { query } : {};
      return new Immutable.Map(defaults)
        .merge(config)
        .merge(filterMap)
        .merge(timerangeMap)
        .merge(queryMap)
        .merge(
          {
            id: searchTypeId,
            type: searchType.type,
          },
        );
    });

  return { widgetMapping, searchTypes };
};
