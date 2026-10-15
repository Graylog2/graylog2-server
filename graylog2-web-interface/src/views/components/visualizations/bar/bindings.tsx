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
import BarVisualization from 'views/components/visualizations/bar/BarVisualization';
import BarVisualizationConfig, {
  DEFAULT_BARMODE,
} from 'views/logic/aggregationbuilder/visualizations/BarVisualizationConfig';
import { hasAtLeastOneMetric } from 'views/components/visualizations/validations';
import type {
  AxisType,
  XYVisualizationConfigFormValues,
} from 'views/logic/aggregationbuilder/visualizations/XYVisualization';
import {
  DEFAULT_AXIS_CONFIG,
  axisTypes,
  DEFAULT_AXIS_TYPE,
} from 'views/logic/aggregationbuilder/visualizations/XYVisualization';
import xyAxisConfigFields, { fromAxisConfig } from 'views/components/visualizations/xyAxisConfigFields';

export type BarVisualizationConfigFormValues = XYVisualizationConfigFormValues & {
  barmode: 'group' | 'stack' | 'relative' | 'overlay';
  axisType: AxisType;
};

const BAR_MODES = ['group', 'stack', 'relative', 'overlay'] as const;

const BarModeHelp = () => {
  const { t } = useTranslation('aggregation');

  return (
    <ul>
      {BAR_MODES.map((mode) => (
        <li key={mode}>
          <h4>{t(`vis_bar.modes.${mode}`)}</h4>
          {t(`vis_bar.mode_help.${mode}`)}
        </li>
      ))}
    </ul>
  );
};

const validate = hasAtLeastOneMetric('vis_validation.bar_chart_requires_metric');

const barChart: VisualizationType<
  typeof BarVisualization.type,
  BarVisualizationConfig,
  BarVisualizationConfigFormValues
> = {
  type: BarVisualization.type,
  get displayName() {
    return visT('vis_types.bar_chart');
  },
  component: BarVisualization,
  config: {
    createConfig: () => ({ barmode: DEFAULT_BARMODE, axisType: DEFAULT_AXIS_TYPE, axisConfig: DEFAULT_AXIS_CONFIG }),
    fromConfig: (config: BarVisualizationConfig | undefined) => ({
      barmode: config?.barmode ?? DEFAULT_BARMODE,
      axisType: config?.axisType ?? DEFAULT_AXIS_TYPE,
      ...fromAxisConfig(config),
    }),
    toConfig: (formValues: BarVisualizationConfigFormValues) =>
      BarVisualizationConfig.create(
        formValues.barmode,
        formValues.axisType,
        formValues.showAxisLabels ? formValues.axisConfig : DEFAULT_AXIS_CONFIG,
      ),
    fields: [
      {
        name: 'barmode',
        get title() {
          return visT('vis_bar.mode');
        },
        type: 'select',
        get options() {
          return translatedOptions(BAR_MODES, 'vis_bar.modes');
        },
        required: true,
        helpComponent: BarModeHelp,
      },
      {
        name: 'axisType',
        get title() {
          return visT('vis_common.axis_type');
        },
        type: 'select',
        get options() {
          return translatedOptions(axisTypes, 'vis_common.axis_types');
        },
        required: true,
      },
      ...xyAxisConfigFields,
    ],
  },
  capabilities: ['event-annotations'],
  validate,
};

export default barChart;
