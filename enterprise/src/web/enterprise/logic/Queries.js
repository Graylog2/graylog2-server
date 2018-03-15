import Immutable from 'immutable';
import uuid from 'uuid/v4';

import QueriesActions from 'enterprise/actions/QueriesActions';
// eslint-disable-next-line no-unused-vars
import QueriesStore from 'enterprise/stores/QueriesStore';
import WidgetActions from 'enterprise/actions/WidgetActions';
// eslint-disable-next-line no-unused-vars
import WidgetStore from 'enterprise/stores/WidgetStore';
import { resultHistogram } from 'enterprise/logic/Widget';

export const _defaultQuery = (id) => {
  return {
    id: id,
    query: '',
    rangeType: 'relative',
    rangeParams: Immutable.Map({ range: '300' }),
    fields: Immutable.Set.of('source', 'message'),
  };
};

export const _defaultWidgets = () => {
  return [
    resultHistogram(uuid()),
    //dataTable(uuid()),
    //messageList(uuid(), undefined, ['source', 'message'])
  ];
};

export const createEmptyQuery = (viewId) => {
  const defaultQuery = _defaultQuery(uuid());
  QueriesActions.create(viewId, defaultQuery);
  _defaultWidgets().forEach(widget => WidgetActions.create(viewId, defaultQuery.id, widget));
  return defaultQuery;
};
