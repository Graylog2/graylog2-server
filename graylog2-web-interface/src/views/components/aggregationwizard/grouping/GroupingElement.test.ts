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
import type { WidgetConfigFormValues, GroupingValidationErrors } from 'views/components/aggregationwizard/WidgetConfigForm';

import GroupingElement from './GroupingElement';

describe('GroupByElement', () => {
  describe('Validation', () => {
    const { validate } = GroupingElement;
    const groupBy = { columnRollup: true, groupings: [] } as WidgetConfigFormValues['groupBy'];

    describe('Values', () => {
      const valuesGrouping = {
        id: 'random-id',
        direction: 'row' as const,
        type: 'values' as const,
        fields: ['action'],
        limit: 10,
      };

      it('should add an error if field absence', () => {
        const grouping = {
          ...valuesGrouping,
          fields: undefined,
        };
        const result = validate({ groupBy: { ...groupBy, groupings: [grouping] } }) as { groupBy: GroupingValidationErrors };

        expect(result.groupBy.groupings[0].fields).toBe('Field is required.');
      });

      it('should not add an error if everything is fine', () => {
        const result = validate({ groupBy: { ...groupBy, groupings: [valuesGrouping] } }) as { groupBy: GroupingValidationErrors };

        expect(result.groupBy).toBeUndefined();
      });

      it('should add an error if limit is undefined', () => {
        const grouping = {
          ...valuesGrouping,
          limit: undefined,
        };
        const result = validate({ groupBy: { ...groupBy, groupings: [grouping] } }) as { groupBy: GroupingValidationErrors };

        expect(result.groupBy.groupings[0].limit).toBe('Limit is required.');
      });

      it('should add an error if limit is smaller than 0', () => {
        const grouping = {
          ...valuesGrouping,
          limit: -1,
        };
        const result = validate({ groupBy: { ...groupBy, groupings: [grouping] } }) as { groupBy: GroupingValidationErrors };

        expect(result.groupBy.groupings[0].limit).toBe('Must be greater than 0.');
      });
    });

    describe('Dates', () => {
      const timeGrouping = {
        id: 'random-id',
        type: 'time' as const,
        direction: 'row' as const,
        fields: ['action'],
        interval: { type: 'auto', scaling: 1 } as const,
      };

      it('should add an error if field absence', () => {
        const grouping = {
          ...timeGrouping,
          fields: undefined,
        };
        const result = validate({ groupBy: { ...groupBy, groupings: [grouping] } }) as { groupBy: GroupingValidationErrors };

        expect(result.groupBy.groupings[0].fields).toBe('Field is required.');
      });

      it('should not add an error if everything is fine', () => {
        const result = validate({ groupBy: { ...groupBy, groupings: [timeGrouping] } });

        expect(result.groupBy).toBeUndefined();
      });

      it('should add an error if scaling absence', () => {
        const grouping = {
          ...timeGrouping,
          interval: { type: 'auto', scaling: undefined } as const,
        };
        const result = validate({ groupBy: { ...groupBy, groupings: [grouping] } }) as { groupBy: GroupingValidationErrors };

        expect(result.groupBy.groupings[0].interval).toBe('Scaling is required.');
      });

      it('should add an error if scaling out of range', () => {
        const grouping = {
          ...timeGrouping,
          interval: { type: 'auto', scaling: -1 } as const,
        };
        const result = validate({ groupBy: { ...groupBy, groupings: [grouping] } }) as { groupBy: GroupingValidationErrors };

        expect(result.groupBy.groupings[0].interval).toBe('Must be greater than 0 and smaller or equals 10.');
      });

      it('should add an error if value is out of range', () => {
        const grouping = {
          ...timeGrouping,
          interval: { type: 'timeunit', value: -1, unit: 'seconds' } as const,
        };
        const result = validate({ groupBy: { ...groupBy, groupings: [grouping] } }) as { groupBy: GroupingValidationErrors };

        expect(result.groupBy.groupings[0].interval).toBe('Must be greater than 0.');
      });

      it('should add an error if value is absent', () => {
        const grouping = {
          ...timeGrouping,
          interval: { type: 'timeunit', value: undefined, unit: 'seconds' } as const,
        };
        const result = validate({ groupBy: { ...groupBy, groupings: [grouping] } }) as { groupBy: GroupingValidationErrors };

        expect(result.groupBy.groupings[0].interval).toBe('Value is required.');
      });
    });
  });

  describe('remove grouping', () => {
    const { onRemove } = GroupingElement;
    const groupBy = { columnRollup: true, groupings: [] } as WidgetConfigFormValues['groupBy'];

    it('should remove form values from a grouping', () => {
      const grouping1 = {
        id: 'random-id',
        direction: 'row' as const,
        type: 'values' as const,
        fields: ['action'],
        limit: 15,
      };
      const grouping2 = {
        id: 'random-id',
        direction: 'column' as const,
        type: 'values' as const,
        fields: ['controller'],
        limit: 10,
      };

      const result = onRemove(1, { groupBy: { ...groupBy, groupings: [grouping1, grouping2] } });

      expect(result.groupBy.groupings).toStrictEqual([grouping1]);
    });

    it('should remove form values to the last from a grouping', () => {
      const grouping1 = {
        id: 'random-id',
        direction: 'row' as const,
        type: 'values' as const,
        fields: ['action'],
        limit: 15,
      };
      const result = onRemove(0, { groupBy: { ...groupBy, groupings: [grouping1] } });

      expect(result.groupBy).toStrictEqual({ columnRollup: true, groupings: [] });
    });

    it('should remove nothing if index is not contained', () => {
      const grouping1 = {
        id: 'random-id',
        direction: 'row' as const,
        type: 'values' as const,
        fields: ['action'],
        limit: 15,
      };
      const grouping2 = {
        id: 'random-id',
        direction: 'column' as const,
        type: 'values' as const,
        fields: ['controller'],
        limit: 15,
      };
      const result = onRemove(4, { groupBy: { ...groupBy, groupings: [grouping1, grouping2] } });

      expect(result.groupBy.groupings).toStrictEqual([grouping1, grouping2]);
    });
  });
});
