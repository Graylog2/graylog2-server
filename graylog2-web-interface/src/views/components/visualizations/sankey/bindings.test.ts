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
import type {
  GroupByFormValues,
  MetricFormValues,
  WidgetConfigFormValues,
} from 'views/components/aggregationwizard/WidgetConfigForm';

import sankey from './bindings';

// The visualization component drags in plotly and friends; the validation logic does not need it.
jest.mock('./SankeyVisualization', () => Object.assign(() => null, { type: 'sankey' }));

const validate = sankey.validate!;

const metric = (fn: string): MetricFormValues => ({ function: fn, field: undefined });

const grouping = (id: string, field: string): GroupByFormValues => ({
  id,
  type: 'values',
  direction: 'row',
  fields: [field],
  limit: 10,
});

const formValues = (overrides: Partial<WidgetConfigFormValues> = {}): WidgetConfigFormValues => ({
  groupBy: { columnRollup: false, groupings: [grouping('1', 'a'), grouping('2', 'b')] },
  metrics: [metric('count()')],
  ...overrides,
});

describe('sankey bindings validate', () => {
  it('returns no error for two groupings and a single metric', () => {
    expect(validate(formValues())).toEqual({});
  });

  it('requires at least two grouping fields', () => {
    const values = formValues({ groupBy: { columnRollup: false, groupings: [grouping('1', 'a')] } });

    expect(validate(values)).toEqual({ type: 'Sankey requires at least two grouping fields.' });
  });

  it('rejects more than one metric, since the additional metrics would be ignored', () => {
    const values = formValues({ metrics: [metric('count()'), metric('avg(took_ms)')] });

    expect(validate(values)).toEqual({ type: 'Sankey supports only a single metric.' });
  });

  it('allows zero metrics (links are weighted by occurrence count)', () => {
    expect(validate(formValues({ metrics: [] }))).toEqual({});
  });
});
