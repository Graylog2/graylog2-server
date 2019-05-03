// @flow strict
import { WidgetActions } from 'enterprise/stores/WidgetStore';
import { TitlesActions, TitleTypes } from 'enterprise/stores/TitlesStore';
import handler from './FieldStatisticsHandler';
import FieldType from '../fieldtypes/FieldType';

jest.mock('enterprise/stores/WidgetStore', () => ({
  WidgetActions: {
    create: jest.fn(widget => Promise.resolve(widget)),
  },
}));

jest.mock('enterprise/stores/TitlesStore', () => ({
  TitlesActions: {
    set: jest.fn(() => Promise.resolve()),
  },
  TitleTypes: {
    Widget: 'widget',
  },
}));

const numericFieldType = new FieldType('foo', ['numeric'], []);
const nonNumericFieldType = new FieldType('foo', [], []);

const queryId = 'queryId';
const fieldName = 'foo';

describe('FieldStatisticsHandler', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });
  it('creates field statistics widget for given numeric field', () => {
    return handler(queryId, fieldName, numericFieldType).then(() => {
      expect(WidgetActions.create).toHaveBeenCalled();
      const widget = WidgetActions.create.mock.calls[0][0];

      expect(widget.config.series.map(s => s.function)).toEqual([
        `count(${fieldName})`,
        `sum(${fieldName})`,
        `avg(${fieldName})`,
        `min(${fieldName})`,
        `max(${fieldName})`,
        `stddev(${fieldName})`,
        `variance(${fieldName})`,
        `card(${fieldName})`,
        `percentile(${fieldName})`,
      ]);
    });
  });
  it('creates field statistics widget for given non-numeric field', () => {
    return handler(queryId, fieldName, nonNumericFieldType).then(() => {
      expect(WidgetActions.create).toHaveBeenCalled();
      const widget = WidgetActions.create.mock.calls[0][0];

      expect(widget.config.series.map(s => s.function)).toEqual([
        `count(${fieldName})`,
        `card(${fieldName})`,
      ]);
    });
  });
  it('adds title to generated widget', () => {
    return handler(queryId, fieldName, nonNumericFieldType).then(() => {
      const widget = WidgetActions.create.mock.calls[0][0];

      expect(TitlesActions.set).toHaveBeenCalledWith(TitleTypes.Widget, widget.id, `Field Statistics for ${fieldName}`);
    });
  });
});
