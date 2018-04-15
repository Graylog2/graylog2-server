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
import ViewsActions from '../actions/ViewsActions';
import ViewsStore from '../stores/ViewsStore';

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

  const positions = {
    [histogram.id]: {
      col: 0,
      row: 0,
      height: 2,
      width: 4,
    },
    [messages.id]: {
      col: 0,
      row: 2,
      height: 6,
      width: 6,
    },
  };

  return { titles, widgets, positions };
};

export const createEmptyQuery = (viewId) => {
  const defaultQuery = _defaultQuery(uuid());
  QueriesActions.create(viewId, defaultQuery)
    .then(() => SelectedFieldsActions.set(defaultQuery.id, ['source', 'message']));
  const { titles, widgets, positions } = _defaultWidgets();
  widgets.forEach(widget => WidgetActions.create(viewId, defaultQuery.id, widget));
  ViewsActions.positions(viewId, positions);
  TitlesActions.load(defaultQuery.id, titles);
  return defaultQuery;
};
