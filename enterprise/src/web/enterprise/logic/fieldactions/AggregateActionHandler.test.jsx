// @flow strict
import AggregateActionHandler from './AggregateActionHandler';
import FieldType from '../fieldtypes/FieldType';
import { WidgetActions } from '../../stores/WidgetStore';
import AggregationWidget from '../aggregationbuilder/AggregationWidget';
import Pivot from '../aggregationbuilder/Pivot';
import Widget from '../widgets/Widget';

jest.mock('enterprise/stores/WidgetStore', () => ({ WidgetActions: {} }));
jest.mock('enterprise/components/datatable/DataTable', () => ({ type: 'table' }));

describe('AggregateActionHandler', () => {
  it('uses field type when generating widget', () => {
    WidgetActions.create = jest.fn((widget: Widget) => Promise.resolve(widget));
    AggregateActionHandler('queryId', 'foo', new FieldType('keyword', [], []), {});

    expect(WidgetActions.create).toHaveBeenCalled();
    const widget: AggregationWidget = WidgetActions.create.mock.calls[0][0];
    const { config } = widget;
    expect(config.rowPivots[0]).toEqual(new Pivot('foo', 'values', { limit: 15 }));
  });
});
