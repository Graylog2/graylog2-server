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
import { useTranslation } from 'react-i18next';

import { translatedOptions, visT } from 'views/components/visualizations/visualizationTranslation';
import type { VisualizationType } from 'views/types';
import NumberVisualization from 'views/components/visualizations/number/NumberVisualization';
import NumberVisualizationConfig, {
  DEFAULT_COLOR_PREFERENCE,
} from 'views/logic/aggregationbuilder/visualizations/NumberVisualizationConfig';

type NumberVisualizationConfigFormValues = {
  trend: boolean;
  trend_preference: 'LOWER' | 'NEUTRAL' | 'HIGHER';
  color_preference: 'TREND_ONLY' | 'FULL_WIDGET';
};

const TrendHelp = () => {
  const { t } = useTranslation('aggregation');

  return (
    <>
      <p>{t('vis_number.trend_help_enabled')}</p>
      <p>{t('vis_number.trend_help_previous')}</p>
    </>
  );
};

const singleNumber: VisualizationType<
  typeof NumberVisualization.type,
  NumberVisualizationConfig,
  NumberVisualizationConfigFormValues
> = {
  type: NumberVisualization.type,
  get displayName() {
    return visT('vis_types.single_number');
  },
  component: NumberVisualization,
  config: {
    createConfig: () => ({ color_preference: DEFAULT_COLOR_PREFERENCE }),
    fromConfig: (config: NumberVisualizationConfig | undefined) => ({
      trend: config?.trend,
      trend_preference: config?.trendPreference,
      color_preference: config?.colorPreference ?? DEFAULT_COLOR_PREFERENCE,
    }),
    toConfig: ({
      trend = false,
      trend_preference,
      color_preference = DEFAULT_COLOR_PREFERENCE,
    }: NumberVisualizationConfigFormValues) =>
      NumberVisualizationConfig.create(trend, trend_preference, undefined, color_preference),
    fields: [
      {
        name: 'trend',
        get title() {
          return visT('vis_number.trend');
        },
        type: 'boolean',
        get description() {
          return visT('vis_number.trend_description');
        },
        helpComponent: TrendHelp,
      },
      {
        name: 'trend_preference',
        get title() {
          return visT('vis_number.trend_preference');
        },
        type: 'select',
        get options() {
          return translatedOptions(['LOWER', 'NEUTRAL', 'HIGHER'], 'vis_number.trend_preferences');
        },
        required: true,
        isShown: (formValues: NumberVisualizationConfigFormValues) => formValues?.trend === true,
      },
      {
        name: 'color_preference',
        get title() {
          return visT('vis_number.highlight');
        },
        type: 'select',
        get options() {
          return translatedOptions(['TREND_ONLY', 'FULL_WIDGET'], 'vis_number.highlight_options');
        },
        required: true,
        isShown: (formValues: NumberVisualizationConfigFormValues) =>
          formValues?.trend === true &&
          (formValues?.trend_preference === 'HIGHER' || formValues?.trend_preference === 'LOWER'),
      },
    ],
  },
};

export default singleNumber;
