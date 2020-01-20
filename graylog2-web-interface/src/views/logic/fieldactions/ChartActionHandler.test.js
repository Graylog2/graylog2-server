// @flow strict
import * as Immutable from 'immutable';

import asMock from 'helpers/mocking/AsMock';

import Widget from 'views/logic/widgets/Widget';
import { WidgetActions } from 'views/stores/WidgetStore';
import { FieldTypesStore } from 'views/stores/FieldTypesStore';
import pivotForField from 'views/logic/searchtypes/aggregation/PivotGenerator';
import Series from 'views/logic/aggregationbuilder/Series';
import FieldTypeMapping from '../fieldtypes/FieldTypeMapping';
import FieldType from '../fieldtypes/FieldType';
import ChartActionHandler from './ChartActionHandler';
import { createElasticsearchQueryString } from '../queries/Query';
import AggregationWidget from '../aggregationbuilder/AggregationWidget';

jest.mock('views/stores/FieldTypesStore', () => ({ FieldTypesStore: { getInitialState: jest.fn() } }));
jest.mock('views/stores/WidgetStore', () => ({
  WidgetActions: {
    create: jest.fn(widget => Promise.resolve(widget)),
  },
}));
jest.mock('views/logic/searchtypes/aggregation/PivotGenerator', () => jest.fn());

describe('ChartActionHandler', () => {
  const emptyFieldType = new FieldType('empty', [], []);

  it('uses average function if triggered on field', async () => {
    await ChartActionHandler({ queryId: 'queryId', field: 'somefield', type: emptyFieldType, contexts: {} });

    expect(WidgetActions.create).toHaveBeenCalledWith(expect.objectContaining({
      config: expect.objectContaining({
        series: [Series.forFunction('avg(somefield)')],
      }),
    }));
  });

  it('uses the function itself if it was triggered on one', async () => {
    await ChartActionHandler({ queryId: 'queryId', field: 'max(somefield)', type: emptyFieldType, contexts: {} });

    expect(WidgetActions.create).toHaveBeenCalledWith(expect.objectContaining({
      config: expect.objectContaining({
        series: [Series.forFunction('max(somefield)')],
      }),
    }));
  });

  describe('retrieves field type for `timestamp` field', () => {
    beforeEach(() => {
      // $FlowFixMe this is a mock
      pivotForField.mockReturnValue('PIVOT');
    });
    it('uses Unknown if FieldTypeStore returns nothing', () => {
      FieldTypesStore.getInitialState.mockReturnValue(undefined);

      ChartActionHandler({ queryId: 'queryId', field: 'somefield', type: emptyFieldType, contexts: {} });

      expect(pivotForField).toHaveBeenCalledWith('timestamp', FieldType.Unknown);
    });
    it('uses Unknown if FieldTypeStore returns neither all nor query fields', () => {
      FieldTypesStore.getInitialState.mockReturnValue({
        all: Immutable.List([]),
        queryFields: Immutable.Map({}),
      });

      ChartActionHandler({ queryId: 'queryId', field: 'somefield', type: emptyFieldType, contexts: {} });

      expect(pivotForField).toHaveBeenCalledWith('timestamp', FieldType.Unknown);
    });
    it('from query field types if present', () => {
      const timestampFieldType = new FieldType('date', [], []);
      FieldTypesStore.getInitialState.mockReturnValue({
        all: Immutable.List([]),
        queryFields: Immutable.fromJS({
          queryId: [
            new FieldTypeMapping('otherfield', new FieldType('sometype', [], [])),
            new FieldTypeMapping('somefield', new FieldType('othertype', [], [])),
            new FieldTypeMapping('timestamp', timestampFieldType),
          ],
        }),
      });

      ChartActionHandler({ queryId: 'queryId', field: 'somefield', type: emptyFieldType, contexts: {} });

      expect(pivotForField).toHaveBeenCalledWith('timestamp', timestampFieldType);
    });
    it('from all field types if present', () => {
      const timestampFieldType = new FieldType('date', [], []);
      FieldTypesStore.getInitialState.mockReturnValue({
        all: Immutable.List([
          new FieldTypeMapping('otherfield', new FieldType('sometype', [], [])),
          new FieldTypeMapping('somefield', new FieldType('othertype', [], [])),
          new FieldTypeMapping('timestamp', timestampFieldType),
        ]),
        queryFields: Immutable.fromJS({}),
      });

      ChartActionHandler({ queryId: 'queryId', field: 'somefield', type: emptyFieldType, contexts: {} });

      expect(pivotForField).toHaveBeenCalledWith('timestamp', timestampFieldType);
    });
    it('uses unknown if not in query field types', () => {
      FieldTypesStore.getInitialState.mockReturnValue({
        all: Immutable.List([]),
        queryFields: Immutable.fromJS({
          queryId: [
            new FieldTypeMapping('otherfield', new FieldType('sometype', [], [])),
            new FieldTypeMapping('somefield', new FieldType('othertype', [], [])),
          ],
        }),
      });

      ChartActionHandler({ queryId: 'queryId', field: 'somefield', type: emptyFieldType, contexts: {} });

      expect(pivotForField).toHaveBeenCalledWith('timestamp', FieldType.Unknown);
    });
    it('uses Unknown if not in all field types', () => {
      FieldTypesStore.getInitialState.mockReturnValue({
        all: Immutable.List([
          new FieldTypeMapping('otherfield', new FieldType('sometype', [], [])),
          new FieldTypeMapping('somefield', new FieldType('othertype', [], [])),
        ]),
        queryFields: Immutable.fromJS({}),
      });

      ChartActionHandler({ queryId: 'queryId', field: 'somefield', type: emptyFieldType, contexts: {} });

      expect(pivotForField).toHaveBeenCalledWith('timestamp', FieldType.Unknown);
    });
  });
  describe('Widget creation', () => {
    beforeEach(() => {
      jest.clearAllMocks();
    });
    it('should create widget with filter of original widget', () => {
      const filter = "author: 'Vanth'";
      const origWidget = Widget.builder().filter(filter).build();
      const timestampFieldType = new FieldType('date', [], []);
      FieldTypesStore.getInitialState.mockReturnValue({
        all: Immutable.List([]),
        queryFields: Immutable.fromJS({
          queryId: [
            new FieldTypeMapping('otherfield', new FieldType('sometype', [], [])),
            new FieldTypeMapping('somefield', new FieldType('othertype', [], [])),
            new FieldTypeMapping('timestamp', timestampFieldType),
          ],
        }),
      });

      ChartActionHandler({ queryId: 'queryId', field: 'somefield', type: emptyFieldType, contexts: { widget: origWidget } });

      const widget = asMock(WidgetActions.create).mock.calls[0][0];

      expect(widget.filter).toEqual(filter);
      expect(pivotForField).toHaveBeenCalledWith('timestamp', timestampFieldType);
    });
    it('duplicates query/timerange/streams/filter of original widget', () => {
      const origWidget = Widget.builder()
        .filter('author: "Vanth"')
        .query(createElasticsearchQueryString('foo:42'))
        .streams(['stream1', 'stream23'])
        .timerange({ type: 'relative', range: 3600 })
        .build();

      ChartActionHandler({
        queryId: 'queryId',
        field: 'foo',
        type: new FieldType('keyword', [], []),
        contexts: { widget: origWidget },
      });

      expect(WidgetActions.create).toHaveBeenCalled();
      const { filter, query, streams, timerange }: AggregationWidget = asMock(WidgetActions.create).mock.calls[0][0];
      expect(filter).toEqual('author: "Vanth"');
      expect(query).toEqual(createElasticsearchQueryString('foo:42'));
      expect(streams).toEqual(['stream1', 'stream23']);
      expect(timerange).toEqual({ type: 'relative', range: 3600 });
    });
  });
});
