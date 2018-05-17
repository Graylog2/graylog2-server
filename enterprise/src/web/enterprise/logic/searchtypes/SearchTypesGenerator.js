import Immutable from 'immutable';
import uuid from 'uuid/v4';

import { widgetDefinition } from '../Widget';
import { searchTypeDefinition } from '../SearchType';

export default (widgets) => {
  let widgetMapping = Immutable.Map();
  const searchTypes = widgets.map(widget => widgetDefinition(widget.type).searchTypes(widget.config).map(searchType => Object.assign(searchType, { widgetId: widget.id })))
    .reduce((acc, cur) => acc.merge(cur), Immutable.Set())
    .map((searchType) => {
      const searchTypeId = uuid();
      widgetMapping = widgetMapping.update(searchType.widgetId, new Immutable.Set(), widgetSearchTypes => widgetSearchTypes.add(searchTypeId));
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
    .add(Immutable.Map({
      id: uuid(),
      type: 'messages',
      limit: 150,
      offset: 0,
    }));

  return { widgetMapping, searchTypes };
};
