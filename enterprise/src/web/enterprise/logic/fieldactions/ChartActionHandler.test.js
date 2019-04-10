import * as Immutable from 'immutable';

import { FieldTypesStore } from 'enterprise/stores/FieldTypesStore';
import pivotForField from 'enterprise/logic/searchtypes/aggregation/PivotGenerator';
import FieldTypeMapping from '../fieldtypes/FieldTypeMapping';
import FieldType from '../fieldtypes/FieldType';
import ChartActionHandler from './ChartActionHandler';

jest.mock('enterprise/stores/FieldTypesStore', () => ({ FieldTypesStore: { getInitialState: jest.fn() } }));
jest.mock('enterprise/stores/WidgetStore', () => ({ WidgetActions: { create: jest.fn() } }));
jest.mock('enterprise/logic/searchtypes/aggregation/PivotGenerator', () => jest.fn());

describe('ChartActionHandler', () => {
  describe('retrieves field type for `timestamp` field', () => {
    beforeEach(() => {
      pivotForField.mockReturnValue('PIVOT');
    });
    it('uses Unknown if FieldTypeStore returns nothing', () => {
      FieldTypesStore.getInitialState.mockReturnValue(undefined);

      ChartActionHandler('queryId', 'somefield');

      expect(pivotForField).toHaveBeenCalledWith('timestamp', FieldType.Unknown);
    });
    it('uses Unknown if FieldTypeStore returns neither all nor query fields', () => {
      FieldTypesStore.getInitialState.mockReturnValue({
        all: Immutable.List([]),
        queryFields: Immutable.Map({}),
      });

      ChartActionHandler('queryId', 'somefield');

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

      ChartActionHandler('queryId', 'somefield');

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

      ChartActionHandler('queryId', 'somefield');

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

      ChartActionHandler('queryId', 'somefield');

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

      ChartActionHandler('queryId', 'somefield');

      expect(pivotForField).toHaveBeenCalledWith('timestamp', FieldType.Unknown);
    });
  });
});
