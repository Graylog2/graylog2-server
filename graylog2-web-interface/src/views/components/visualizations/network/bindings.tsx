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
import NetworkGraphVisualization from 'views/components/visualizations/network/NetworkGraphVisualization';
import NetworkVisualizationConfig, {
  COLORSCALES,
} from 'views/logic/aggregationbuilder/visualizations/NetworkVisualizationConfig';
import { defaultCompare } from 'logic/DefaultCompare';
import type { WidgetConfigFormValues } from 'views/components/aggregationwizard';

type NetworkVisualizationConfigFormValues = {
  colorScale: (typeof COLORSCALES)[number];
  reverseScale: boolean;
};

const countGroupingFields = (formValues: WidgetConfigFormValues) =>
  (formValues.groupBy?.groupings ?? []).reduce((total, grouping) => total + (grouping.fields?.length ?? 0), 0);

const validate = (formValues: WidgetConfigFormValues) => {
  if (countGroupingFields(formValues) < 2) {
    return { type: 'Network graph requires at least two grouping fields.' };
  }

  return {};
};

const networkGraph: VisualizationType<
  typeof NetworkGraphVisualization.type,
  NetworkVisualizationConfig,
  NetworkVisualizationConfigFormValues
> = {
  type: NetworkGraphVisualization.type,
  displayName: 'Network Graph',
  component: NetworkGraphVisualization,
  config: {
    fromConfig: (config: NetworkVisualizationConfig = NetworkVisualizationConfig.empty()) => ({
      colorScale: config.colorScale,
      reverseScale: config.reverseScale,
    }),
    toConfig: ({ colorScale, reverseScale = false }: NetworkVisualizationConfigFormValues) =>
      NetworkVisualizationConfig.create(colorScale, reverseScale),
    createConfig: () => ({ colorScale: 'YlOrRd', reverseScale: false }),
    fields: [
      {
        name: 'colorScale',
        title: 'Color Scale',
        required: true,
        type: 'select',
        options: [...COLORSCALES].sort(defaultCompare),
      },
      {
        name: 'reverseScale',
        type: 'boolean',
        title: 'Reverse Scale',
      },
    ],
  },
  validate,
};

export default networkGraph;
