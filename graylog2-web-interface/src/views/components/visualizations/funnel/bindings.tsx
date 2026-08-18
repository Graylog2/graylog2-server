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
import type { WidgetConfigFormValues } from 'views/components/aggregationwizard';
import FunnelVisualizationConfig, {
  DEFAULT_FUNNEL_START_COLOR,
  DEFAULT_FUNNEL_END_COLOR,
} from 'views/logic/aggregationbuilder/visualizations/FunnelVisualizationConfig';

import FunnelVisualization from './FunnelVisualization';
import FunnelColorPickerField from './FunnelColorPickerField';

type FunnelVisualizationConfigFormValues = {
  startColor: string;
  endColor: string;
};

const countGroupingFields = (formValues: WidgetConfigFormValues) =>
  (formValues.groupBy?.groupings ?? []).reduce((total, grouping) => total + (grouping.fields?.length ?? 0), 0);

const validate = (formValues: WidgetConfigFormValues) => {
  if (countGroupingFields(formValues) < 1) {
    return { type: 'Funnel requires at least one grouping field.' };
  }

  if ((formValues.metrics?.length ?? 0) > 1) {
    return { type: 'Funnel supports only a single metric.' };
  }

  return {};
};

const funnelChart: VisualizationType<typeof FunnelVisualization.type, FunnelVisualizationConfig, FunnelVisualizationConfigFormValues> = {
  type: FunnelVisualization.type,
  displayName: 'Funnel',
  component: FunnelVisualization,
  config: {
    createConfig: () => ({
      startColor: DEFAULT_FUNNEL_START_COLOR,
      endColor: DEFAULT_FUNNEL_END_COLOR,
    }),
    fromConfig: (vizConfig?: FunnelVisualizationConfig) => ({
      startColor: vizConfig?.startColor ?? DEFAULT_FUNNEL_START_COLOR,
      endColor: vizConfig?.endColor ?? DEFAULT_FUNNEL_END_COLOR,
    }),
    toConfig: (formValues: FunnelVisualizationConfigFormValues) =>
      FunnelVisualizationConfig.create(formValues.startColor, formValues.endColor),
    fields: [
      {
        type: 'custom',
        name: 'startColor',
        title: 'Start color',
        id: 'funnel-start-color',
        component: FunnelColorPickerField,
      },
      {
        type: 'custom',
        name: 'endColor',
        title: 'End color',
        id: 'funnel-end-color',
        component: FunnelColorPickerField,
      },
    ],
  },
  validate,
};

export default funnelChart;
