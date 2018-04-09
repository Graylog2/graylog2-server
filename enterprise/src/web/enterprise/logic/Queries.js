import Immutable from 'immutable';
import uuid from 'uuid/v4';

import { messageList, resultHistogram } from 'enterprise/logic/Widget';

import SelectedFieldsActions from '../actions/SelectedFieldsActions';
// eslint-disable-next-line no-unused-vars
import SelectedFieldsStore from '../stores/SelectedFieldsStore';

// eslint-disable-next-line no-unused-vars
import CurrentViewStore from '../stores/CurrentViewStore';

import QueriesActions from 'enterprise/actions/QueriesActions';
// eslint-disable-next-line no-unused-vars
import QueriesStore from 'enterprise/stores/QueriesStore';

// eslint-disable-next-line no-unused-vars
import TitlesActions from '../actions/TitlesActions';
import TitlesStore from '../stores/TitlesStore';

import WidgetActions from 'enterprise/actions/WidgetActions';
// eslint-disable-next-line no-unused-vars
import WidgetStore from 'enterprise/stores/WidgetStore';

export const _defaultQuery = (id) => {
  return {
    id: id,
    query: '',
    rangeType: 'relative',
    rangeParams: Immutable.Map({ range: '300' }),
  };
};

export const _defaultWidgets = () => {
  const histogram = resultHistogram(uuid());
  const messages = messageList(uuid());
  const widgets = [
    histogram,
    messages,
  ];

  const titles = {
    widget: {
      [histogram.id]: 'Message Count',
      [messages.id]: 'Messages',
    },
  };

  return { titles, widgets };
};

export const createEmptyQuery = (viewId) => {
  const defaultQuery = _defaultQuery(uuid());
  QueriesActions.create(viewId, defaultQuery)
    .then(() => SelectedFieldsActions.set(defaultQuery.id, ['source', 'message']));
  const { titles, widgets } = _defaultWidgets();
  widgets.forEach(widget => WidgetActions.create(viewId, defaultQuery.id, widget));
  TitlesActions.load(defaultQuery.id, titles);
  return defaultQuery;
};
