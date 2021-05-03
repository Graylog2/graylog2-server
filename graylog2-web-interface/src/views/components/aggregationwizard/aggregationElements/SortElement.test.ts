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
import { SortFormValues, WidgetConfigFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';

import SortElement from './SortElement';

describe('SortElement', () => {
  describe('Validation', () => {
    const { validate } = SortElement;

    it('should assert field is present', () => {
      const values: WidgetConfigFormValues = { sort: [{ direction: 'Ascending', id: 'foob' }] };
      const result = validate(values);

      expect(result.sort[0].field).toBe('Field is required.');
    });

    it('should not fail if field and direction is present', () => {
      const values: WidgetConfigFormValues = { sort: [{ field: 'action', direction: 'Ascending', id: 'foob' }] };
      const result = validate(values);

      expect(result.sort).toBeUndefined();
    });

    it('should assert direction is present', () => {
      const values: WidgetConfigFormValues = { sort: [{ field: 'action', id: 'foob' }] };
      const result = validate(values);

      expect(result.sort[0].direction).toBe('Direction is required.');
    });

    it('should show an error if a time based group by and a non datatable is present', () => {
      const values: WidgetConfigFormValues = {
        sort: [{ field: 'action', direction: 'Ascending', id: 'foob' }],
        groupBy: {
          columnRollup: false,
          groupings:
            [{
              id: 'foob',
              direction: 'row',
              field: { field: 'time', type: 'time' },
              interval: { type: 'timeunit', value: 3, unit: 'seconds' },
            }],
        },
        visualization: { type: 'chart' },
      };
      const result = validate(values);

      expect(result.sort[0].field).toBe('Sort on non data table with time based row grouping does not work.');
    });
  });
});
