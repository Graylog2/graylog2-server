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
import { MetricFormValues, WidgetConfigFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';

import MetricElement from './MetricElement';

describe('MetricElement', () => {
  describe('Remove section from form', () => {
    const { removeElement } = MetricElement;
    const metric1 = { function: 'count', field: undefined } as MetricFormValues;
    const metric2 = { function: 'avg', field: 'took_ms' } as MetricFormValues;

    it('should remove an metric from the form', () => {
      const values = { metrics: [metric1, metric2] } as WidgetConfigFormValues;
      const result = removeElement(1, values);

      expect(result.metrics).toStrictEqual([metric1]);
    });

    it('should remove the last metric from the form', () => {
      const values = { metrics: [metric2] } as WidgetConfigFormValues;
      const result = removeElement(0, values);

      expect(result.metrics).toStrictEqual([]);
    });

    it('should remove no metric from the form if the index does not fit', () => {
      const values = { metrics: [metric1, metric2] } as WidgetConfigFormValues;
      const result = removeElement(3, values);

      expect(result.metrics).toStrictEqual([metric1, metric2]);
    });
  });
});
