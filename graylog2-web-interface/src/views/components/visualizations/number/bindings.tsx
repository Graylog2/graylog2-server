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
import * as React from 'react';

import type { VisualizationType } from 'views/types';
import NumberVisualization from 'views/components/visualizations/number/NumberVisualization';
import NumberVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/NumberVisualizationConfig';

type NumberVisualizationConfigFormValues = {
  trend: boolean,
  trend_preference: 'LOWER' | 'NEUTRAL' | 'HIGHER',
};

const singleNumber: VisualizationType<typeof NumberVisualization.type, NumberVisualizationConfig, NumberVisualizationConfigFormValues> = {
  type: NumberVisualization.type,
  displayName: 'Single Number',
  component: NumberVisualization,
  config: {
    fromConfig: (config: NumberVisualizationConfig | undefined) => ({ trend: config?.trend, trend_preference: config?.trendPreference }),
    toConfig: ({ trend = false, trend_preference }: NumberVisualizationConfigFormValues) => NumberVisualizationConfig.create(trend, trend_preference),
    fields: [{
      name: 'trend',
      title: 'Trend',
      type: 'boolean',
      description: 'Show trend information for this number.',
      helpComponent: () => (
        <>
          <p>
            If the user enables trending, a separate box is shown below the current value, indicating the direction of the change
            by an icon as well as the absolute and the relative differences between the current value and the previous one.
          </p>

          <p>
            The previous value is calculated by performing two searches in the background, which are completely identical besides
            the timerange. The timerange of the first search is identical to the one configured for this query/this widget,
            the second one is the same timerange, but with an offset of the timerange length shifted to the past.
          </p>
        </>
      ),
    }, {
      name: 'trend_preference',
      title: 'Trend Preference',
      type: 'select',
      options: [['Lower', 'LOWER'], ['Neutral', 'NEUTRAL'], ['Higher', 'HIGHER']],
      required: true,
      isShown: (formValues: NumberVisualizationConfigFormValues) => formValues?.trend === true,
    }],
  },
};

export default singleNumber;
