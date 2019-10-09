// @flow strict
import FieldType from 'views/logic/fieldtypes/FieldType';

import asMock from 'helpers/mocking/AsMock';
import { WidgetActions } from 'views/stores/WidgetStore';
import ShowDocumentsHandler from './ShowDocumentsHandler';
import AggregationWidget from '../aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from '../aggregationbuilder/AggregationWidgetConfig';
import PivotGenerator from '../searchtypes/aggregation/PivotGenerator';

jest.mock('views/stores/WidgetStore', () => ({
  WidgetActions: {
    create: jest.fn(widget => Promise.resolve(widget)),
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
        expect(newWidget.filter).toEqual('');
      });
  });
  it('adds the given value path as widget filter for new message widget', () => {
    return ShowDocumentsHandler({ queryId, field, value: 42, type: FieldType.Unknown, contexts: { widget: widget, valuePath: [{ bar: 42 }, { [field]: 'Hello!' }] } })
      .then(() => {
        expect(WidgetActions.create).toHaveBeenCalled();
        const newWidget = asMock(WidgetActions.create).mock.calls[0][0];
        expect(newWidget.filter).toEqual('foo:Hello\\! AND bar:42');
      });
  });
  it('adds the given value path to an existing widget filter', () => {
    const widgetWithFilter = widget.toBuilder().filter('baz:23').build();
    return ShowDocumentsHandler({ queryId, field, value: 42, type: FieldType.Unknown, contexts: { widget: widgetWithFilter, valuePath: [{ bar: 42 }, { [field]: 'Hello!' }] } })
      .then(() => {
        expect(WidgetActions.create).toHaveBeenCalled();
        const newWidget = asMock(WidgetActions.create).mock.calls[0][0];
        expect(newWidget.filter).toEqual('baz:23 AND foo:Hello\\! AND bar:42');
      });
  });
  it('deduplicates widget filter', () => {
    const widgetWithFilter = widget.toBuilder().filter('bar:42').build();
    return ShowDocumentsHandler({ queryId, field, value: 42, type: FieldType.Unknown, contexts: { widget: widgetWithFilter, valuePath: [{ bar: 42 }, { [field]: 'Hello!' }] } })
      .then(() => {
        expect(WidgetActions.create).toHaveBeenCalled();
        const newWidget = asMock(WidgetActions.create).mock.calls[0][0];
        expect(newWidget.filter).toEqual('bar:42 AND foo:Hello\\!');
      });
  });
});
