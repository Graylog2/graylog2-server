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
import type { VisualizationType } from 'views/types';
import SankeyVisualization from 'views/components/visualizations/sankey/SankeyVisualization';
import type { WidgetConfigFormValues } from 'views/components/aggregationwizard';

const countGroupingFields = (formValues: WidgetConfigFormValues) =>
  (formValues.groupBy?.groupings ?? []).reduce((total, grouping) => total + (grouping.fields?.length ?? 0), 0);

const validate = (formValues: WidgetConfigFormValues) => {
  if (countGroupingFields(formValues) < 2) {
    return { type: 'Sankey requires at least two grouping fields.' };
  }

  if ((formValues.metrics?.length ?? 0) > 1) {
    return { type: 'Sankey supports only a single metric.' };
  }

  return {};
};

const sankey: VisualizationType<typeof SankeyVisualization.type> = {
  type: SankeyVisualization.type,
  displayName: 'Sankey',
  component: SankeyVisualization,
  validate,
};

export default sankey;
