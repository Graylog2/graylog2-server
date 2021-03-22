import * as React from 'react';
import type { VisualizationType } from 'views/types';

import NumberVisualization from 'views/components/visualizations/number/NumberVisualization';
import NumberVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/NumberVisualizationConfig';
import { NumberVisualizationConfigFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';

const singleNumber: VisualizationType = {
  type: NumberVisualization.type,
  displayName: 'Single Number',
  component: NumberVisualization,
  config: {
    fromConfig: (config: NumberVisualizationConfig | undefined): NumberVisualizationConfigFormValues => ({ trend: config?.trend, trend_preference: config?.trendPreference }),
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
