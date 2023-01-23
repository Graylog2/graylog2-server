/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import type { GroupByFormValues, WidgetConfigFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';

import GroupingElement from './GroupingElement';

describe('GroupByElement', () => {
  describe('Validation', () => {
    const { validate } = GroupingElement;
    const groupBy = { columnRollup: true, groupings: [] } as WidgetConfigFormValues['groupBy'];

    describe('Values', () => {
      it('should add an error if field absence', () => {
        const grouping = { direction: 'row', fields: undefined, limit: 10 } as GroupByFormValues;
        const result = validate({ groupBy: { ...groupBy, groupings: [grouping] } });

        expect(result.groupBy.groupings[0].fields).toBe('Field is required.');
      });

      it('should not add an error if everything is fine', () => {
        const grouping = { direction: 'row', fields: ['action'], limit: 10 } as GroupByFormValues;
        const result = validate({ groupBy: { ...groupBy, groupings: [grouping] } });

        expect(result.groupBy).toBeUndefined();
      });

      it('should add an error if limit is undefined', () => {
        const grouping = { direction: 'row', fields: ['action'], limit: undefined } as GroupByFormValues;
        const result = validate({ groupBy: { ...groupBy, groupings: [grouping] } });

        expect(result.groupBy.groupings[0].limit).toBe('Limit is required.');
      });

      it('should add an error if limit is smaller than 0', () => {
        const grouping = { direction: 'row', fields: ['action'], limit: -1 } as GroupByFormValues;
        const result = validate({ groupBy: { ...groupBy, groupings: [grouping] } });

        expect(result.groupBy.groupings[0].limit).toBe('Must be greater than 0.');
      });
    });

    describe('Dates', () => {
      it('should add an error if field absence', () => {
        const grouping = { direction: 'row', fields: undefined, interval: { type: 'auto', scaling: 1 } } as GroupByFormValues;
        const result = validate({ groupBy: { ...groupBy, groupings: [grouping] } });

        expect(result.groupBy.groupings[0].fields).toBe('Field is required.');
      });

      it('should not add an error if everything is fine', () => {
        const grouping = { direction: 'row', fields: ['action'], interval: { type: 'auto', scaling: 1 } } as GroupByFormValues;
        const result = validate({ groupBy: { ...groupBy, groupings: [grouping] } });

        expect(result.groupBy).toBeUndefined();
      });

      it('should add an error if scaling absence', () => {
        const grouping = { direction: 'row', fields: undefined, interval: { type: 'auto', scaling: undefined } } as GroupByFormValues;
        const result = validate({ groupBy: { ...groupBy, groupings: [grouping] } });

        expect(result.groupBy.groupings[0].interval).toBe('Scaling is required.');
      });

      it('should add an error if scaling out of range', () => {
        const grouping = { direction: 'row', fields: undefined, interval: { type: 'auto', scaling: -1 } } as GroupByFormValues;
        const result = validate({ groupBy: { ...groupBy, groupings: [grouping] } });

        expect(result.groupBy.groupings[0].interval).toBe('Must be greater than 0 and smaller or equals 10.');
      });

      it('should add an error if value is out of range', () => {
        const grouping = { direction: 'row', fields: undefined, interval: { type: 'timeunit', value: -1, unit: 'seconds' } } as GroupByFormValues;
        const result = validate({ groupBy: { ...groupBy, groupings: [grouping] } });

        expect(result.groupBy.groupings[0].interval).toBe('Must be greater than 0.');
      });

      it('should add an error if value is absent', () => {
        const grouping = { direction: 'row', fields: undefined, interval: { type: 'timeunit', value: undefined, unit: 'seconds' } } as GroupByFormValues;
        const result = validate({ groupBy: { ...groupBy, groupings: [grouping] } });

        expect(result.groupBy.groupings[0].interval).toBe('Value is required.');
      });
    });
  });

  describe('remove grouping', () => {
    const { onRemove } = GroupingElement;
    const groupBy = { columnRollup: true, groupings: [] } as WidgetConfigFormValues['groupBy'];

    it('should remove form values from a grouping', () => {
      const grouping1 = { direction: 'row', fields: ['action'], limit: 15 } as GroupByFormValues;
      const grouping2 = { direction: 'column', fields: ['controller'], limit: 10 } as GroupByFormValues;

      const result = onRemove(1, { groupBy: { ...groupBy, groupings: [grouping1, grouping2] } });

      expect(result.groupBy.groupings).toStrictEqual([grouping1]);
    });

    it('should remove form values to the last from a grouping', () => {
      const grouping1 = { direction: 'row', fields: ['action'], limit: 15 } as GroupByFormValues;
      const result = onRemove(0, { groupBy: { ...groupBy, groupings: [grouping1] } });

      expect(result.groupBy).toStrictEqual({ columnRollup: true, groupings: [] });
    });

    it('should remove nothing if index is not contained', () => {
      const grouping1 = { direction: 'row', fields: ['action'], limit: 15 } as GroupByFormValues;
      const grouping2 = { direction: 'column', fields: ['controller'], limit: 15 } as GroupByFormValues;
      const result = onRemove(4, { groupBy: { ...groupBy, groupings: [grouping1, grouping2] } });

      expect(result.groupBy.groupings).toStrictEqual([grouping1, grouping2]);
    });
  });
});
