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
      .searchTypes(widget)
      .map(searchType => Object.assign(
        {},
        { id: uuid(), timerange: widget.timerange, query: widget.query, streams: widget.streams },
        searchType,
        { widgetId: widget.id },
        filterForWidget(widget),
      )))
    .reduce((acc, cur) => acc.merge(cur), Immutable.Set())
    .map((searchType) => {
      widgetMapping = widgetMapping.update(searchType.widgetId, new Immutable.Set(), widgetSearchTypes => widgetSearchTypes.add(searchType.id));
      const typeDefinition = searchTypeDefinition(searchType.type);
      if (!typeDefinition || !typeDefinition.defaults) {
        // eslint-disable-next-line no-console
        console.warn(`Unable to find type definition or defaults for search type ${searchType.type} - skipping!`);
      }
      const { defaults = {} } = typeDefinition || {};
      const { config, filter, timerange, query, streams } = searchType;
      const filterMap = filter ? { filter } : {};
      const timerangeMap = timerange ? { timerange } : {};
      const queryMap = query ? { query } : {};
      const streamsMap = streams ? { streams } : {};
      const nameMap = searchType.name ? { name: searchType.name } : {};
      return new Immutable.Map(defaults)
        .merge(config)
        .merge(filterMap)
        .merge(timerangeMap)
        .merge(queryMap)
        .merge(streamsMap)
        .merge(nameMap)
        .merge(
          {
            id: searchType.id,
            type: searchType.type,
          },
        );
    });

  return { widgetMapping, searchTypes };
};
