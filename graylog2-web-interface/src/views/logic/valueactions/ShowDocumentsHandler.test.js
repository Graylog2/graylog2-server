// @flow strict
import FieldType from 'views/logic/fieldtypes/FieldType';

import asMock from 'helpers/mocking/AsMock';
import { TitlesActions } from 'views/stores/TitlesStore';
import TitleTypes from 'views/stores/TitleTypes';
import View from 'views/logic/views/View';
import { WidgetActions } from 'views/stores/WidgetStore';
import ShowDocumentsHandler from './ShowDocumentsHandler';
import AggregationWidget from '../aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from '../aggregationbuilder/AggregationWidgetConfig';
import PivotGenerator from '../searchtypes/aggregation/PivotGenerator';
import { createElasticsearchQueryString } from '../queries/Query';

jest.mock('views/stores/WidgetStore', () => ({
  WidgetActions: {
    create: jest.fn(widget => Promise.resolve(widget)),
  },
}));

jest.mock('views/stores/TitlesStore', () => ({
  TitlesActions: {
    set: jest.fn(() => Promise.resolve()),
  },
}));

const queryId = 'someQuery';
const field = 'foo';

const widgetConfig = AggregationWidgetConfig
  .builder()
  .rowPivots([
    PivotGenerator('bar', FieldType.Unknown),
    PivotGenerator(field, FieldType.Unknown),
  ]);
const widget = AggregationWidget.builder().newId().config(widgetConfig).build();

describe('ShowDocumentsHandler', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });
  it('adds a new message widget', () => {
    return ShowDocumentsHandler({ queryId, field, value: 42, type: FieldType.Unknown, contexts: { widget: widget, valuePath: [{ bar: 42 }, { [field]: 'Hello!' }] } })
      .then(() => {
        expect(WidgetActions.create).toHaveBeenCalled();
      });
  });
  it('adds a new message widget for an empty value path', () => {
    return ShowDocumentsHandler({ queryId, field, value: 42, type: FieldType.Unknown, contexts: { widget: widget, valuePath: [] } })
      .then(() => {
        expect(WidgetActions.create).toHaveBeenCalled();
        const newWidget = asMock(WidgetActions.create).mock.calls[0][0];
        expect(newWidget.query).toEqual(createElasticsearchQueryString());
      });
  });
  it('adds the given value path as widget filter for new message widget', () => {
    return ShowDocumentsHandler({ queryId, field, value: 42, type: FieldType.Unknown, contexts: { widget: widget, valuePath: [{ bar: 42 }, { [field]: 'Hello!' }] } })
      .then(() => {
        expect(WidgetActions.create).toHaveBeenCalled();
        const newWidget = asMock(WidgetActions.create).mock.calls[0][0];
        expect(newWidget.query).toEqual(createElasticsearchQueryString('foo:Hello\\! AND bar:42'));
      });
  });
  it('adds the given value path to an existing widget query', () => {
    const widgetWithFilter = widget.toBuilder().query(createElasticsearchQueryString('baz:23')).build();
    return ShowDocumentsHandler({ queryId, field, value: 42, type: FieldType.Unknown, contexts: { widget: widgetWithFilter, valuePath: [{ bar: 42 }, { [field]: 'Hello!' }] } })
      .then(() => {
        expect(WidgetActions.create).toHaveBeenCalled();
        const newWidget = asMock(WidgetActions.create).mock.calls[0][0];
        expect(newWidget.query).toEqual(createElasticsearchQueryString('baz:23 AND foo:Hello\\! AND bar:42'));
      });
  });
  it('sets title for new messages widget', () => {
    const widgetWithFilter = widget.toBuilder().query(createElasticsearchQueryString('foo:23')).build();
    return ShowDocumentsHandler({ queryId, field: 'hello', value: 'world', type: FieldType.Unknown, contexts: { widget: widgetWithFilter, valuePath: [{ bar: 42 }, { hello: 'world' }] } })
      .then(() => {
        const newWidget = asMock(WidgetActions.create).mock.calls[0][0];
        expect(TitlesActions.set).toHaveBeenCalledWith(TitleTypes.Widget, newWidget.id, 'Messages for hello:world AND bar:42');
      });
  });
  describe('on dashboard', () => {
    const view = View.builder().type(View.Type.Dashboard).build();
    it('copies timerange of original widget', () => {
      const widgetWithFilter = widget.toBuilder()
        .timerange({ type: 'relative', range: 1800 })
        .query(createElasticsearchQueryString())
        .build();
      return ShowDocumentsHandler({
        queryId,
        field: 'hello',
        value: 'world',
        type: FieldType.Unknown,
        contexts: { widget: widgetWithFilter, valuePath: [{ bar: 42 }, { hello: 'world' }], view },
      })
        .then(() => {
          const newWidget = asMock(WidgetActions.create).mock.calls[0][0];
          expect(newWidget.timerange).toEqual({ type: 'relative', range: 1800 });
        });
    });
    it('copies timerange of original widget', () => {
      const widgetWithFilter = widget.toBuilder()
        .streams(['deadbeef', 'cafecafe'])
        .query(createElasticsearchQueryString())
        .build();
      return ShowDocumentsHandler({
        queryId,
        field: 'hello',
        value: 'world',
        type: FieldType.Unknown,
        contexts: { widget: widgetWithFilter, valuePath: [{ bar: 42 }, { hello: 'world' }], view },
      })
        .then(() => {
          const newWidget = asMock(WidgetActions.create).mock.calls[0][0];
          expect(newWidget.streams).toEqual(['deadbeef', 'cafecafe']);
        });
    });
  });
});
