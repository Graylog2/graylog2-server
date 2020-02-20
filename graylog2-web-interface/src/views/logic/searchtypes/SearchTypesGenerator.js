// @flow strict
import * as Immutable from 'immutable';
import uuid from 'uuid/v4';
import Widget from 'views/logic/widgets/Widget';

import { widgetDefinition } from '../Widgets';
import searchTypeDefinition from '../SearchType';
import type { WidgetMapping } from '../views/View';

const filterForWidget = widget => (widget.filter ? { filter: { type: 'query_string', query: widget.filter } } : {});

export type ResultType = {
  searchTypes: Immutable.Set<Immutable.Map<string, any>>,
  widgetMapping: WidgetMapping,
};

export default (widgets: (Array<Widget> | Immutable.List<Widget>)): ResultType => {
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
      const { config, widgetId, ...rest } = searchType;
      return Immutable.Map(defaults)
        .merge(rest)
        .merge(config)
        .merge(
          {
            id: searchType.id,
            type: searchType.type,
          },
        );
    });

  return { widgetMapping, searchTypes };
};
