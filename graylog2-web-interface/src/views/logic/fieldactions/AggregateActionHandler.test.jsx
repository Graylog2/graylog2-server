// @flow strict
import asMock from 'helpers/mocking/AsMock';
import mockAction from 'helpers/mocking/MockAction';

import { WidgetActions } from 'views/stores/WidgetStore';
import AggregateActionHandler from './AggregateActionHandler';
import FieldType from '../fieldtypes/FieldType';
import AggregationWidget from '../aggregationbuilder/AggregationWidget';
import Pivot from '../aggregationbuilder/Pivot';
import Widget from '../widgets/Widget';

jest.mock('views/stores/WidgetStore', () => ({ WidgetActions: {} }));
jest.mock('views/components/datatable/DataTable', () => ({ type: 'table' }));

describe('AggregateActionHandler', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });
  it('uses field type when generating widget', () => {
    WidgetActions.create = mockAction(jest.fn((widget: Widget) => Promise.resolve(widget)));
    AggregateActionHandler({ queryId: 'queryId', field: 'foo', type: new FieldType('keyword', [], []), contexts: {} });

    expect(WidgetActions.create).toHaveBeenCalled();
    const widget: AggregationWidget = asMock(WidgetActions.create).mock.calls[0][0];
    const { config } = widget;
    expect(config.rowPivots[0]).toEqual(new Pivot('foo', 'values', { limit: 15 }));
  });

  it('uses field type when generating widget', () => {
    WidgetActions.create = mockAction(jest.fn((widget: Widget) => Promise.resolve(widget)));
    const filter = "author: 'Vanth'";
    const origWidget = Widget.builder().filter(filter).build();
    AggregateActionHandler({ queryId: 'queryId', field: 'foo', type: new FieldType('keyword', [], []), contexts: { widget: origWidget } });

    expect(WidgetActions.create).toHaveBeenCalled();
    const widget: AggregationWidget = asMock(WidgetActions.create).mock.calls[0][0];
    expect(widget.filter).toEqual(filter);
  });
});
