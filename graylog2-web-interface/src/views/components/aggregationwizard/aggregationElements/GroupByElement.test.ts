import { GroupByFormValues, WidgetConfigFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';

import GroupByElement from './GroupByElement';

describe('GroupByElement', () => {
  describe('Validation', () => {
    const { validate } = GroupByElement;
    const values = { groupBy: { columnRollup: true, groupings: [] } } as WidgetConfigFormValues;

    describe('Values', () => {
      it('should add an error if field absence', () => {
        const grouping = { direction: 'row', field: { field: undefined }, limit: 10 } as GroupByFormValues;
        values.groupBy.groupings = [grouping];
        const result = validate(values);

        expect(result.groupBy.groupings[0].field).toBe('Field is required.');
      });

      it('should not add an error if everything is fine', () => {
        const grouping = { direction: 'row', field: { field: 'action' }, limit: 10 } as GroupByFormValues;
        values.groupBy.groupings = [grouping];
        const result = validate(values);

        expect(result.groupBy).toBeUndefined();
      });

      it('should add an error if limit is undefined', () => {
        const grouping = { direction: 'row', field: { field: 'action' }, limit: undefined } as GroupByFormValues;
        values.groupBy.groupings = [grouping];
        const result = validate(values);

        expect(result.groupBy.groupings[0].limit).toBe('Limit is required.');
      });

      it('should add an error if limit is smaller than 0', () => {
        const grouping = { direction: 'row', field: { field: 'action' }, limit: -1 } as GroupByFormValues;
        values.groupBy.groupings = [grouping];
        const result = validate(values);

        expect(result.groupBy.groupings[0].limit).toBe('Must be greater than 0.');
      });
    });

    describe('Dates', () => {
      it('should add an error if field absence', () => {
        const grouping = { direction: 'row', field: { field: undefined }, interval: { type: 'auto', scaling: 1 } } as GroupByFormValues;
        values.groupBy.groupings = [grouping];
        const result = validate(values);

        expect(result.groupBy.groupings[0].field).toBe('Field is required.');
      });

      it('should not add an error if everything is fine', () => {
        const grouping = { direction: 'row', field: { field: 'action' }, interval: { type: 'auto', scaling: 1 } } as GroupByFormValues;
        values.groupBy.groupings = [grouping];
        const result = validate(values);

        expect(result.groupBy).toBeUndefined();
      });

      it('should add an error if scaling absence', () => {
        const grouping = { direction: 'row', field: { field: undefined }, interval: { type: 'auto', scaling: undefined } } as GroupByFormValues;
        values.groupBy.groupings = [grouping];
        const result = validate(values);

        expect(result.groupBy.groupings[0].interval).toBe('Scaling is required.');
      });

      it('should add an error if scaling out of range', () => {
        const grouping = { direction: 'row', field: { field: undefined }, interval: { type: 'auto', scaling: -1 } } as GroupByFormValues;
        values.groupBy.groupings = [grouping];
        const result = validate(values);

        expect(result.groupBy.groupings[0].interval).toBe('Must be greater than 0 and smaller or equals 10.');
      });

      it('should add an error if value is out of range', () => {
        const grouping = { direction: 'row', field: { field: undefined }, interval: { type: 'timeunit', value: -1, unit: 'seconds' } } as GroupByFormValues;
        values.groupBy.groupings = [grouping];
        const result = validate(values);

        expect(result.groupBy.groupings[0].interval).toBe('Must be greater than 0.');
      });

      it('should add an error if value is absent', () => {
        const grouping = { direction: 'row', field: { field: undefined }, interval: { type: 'timeunit', value: undefined, unit: 'seconds' } } as GroupByFormValues;
        values.groupBy.groupings = [grouping];
        const result = validate(values);

        expect(result.groupBy.groupings[0].interval).toBe('Value is required.');
      });
    });
  });
});
