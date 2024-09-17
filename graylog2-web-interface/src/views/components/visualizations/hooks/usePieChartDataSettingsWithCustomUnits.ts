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

import { useCallback } from 'react';

import useFeature from 'hooks/useFeature';
import { NO_FIELD_NAME_SERIES, UNIT_FEATURE_FLAG } from 'views/components/visualizations/Constants';
import useWidgetUnits from 'views/components/visualizations/hooks/useWidgetUnits';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import getFieldNameFromTrace from 'views/components/visualizations/utils/getFieldNameFromTrace';
import { getPieHoverTemplateSettings } from 'views/components/visualizations/utils/chartLayoutGenerators';

export type PieHoverTemplateSettings = { text: Array<string>, hovertemplate: string, meta: string, textinfo: 'percent' };
export type PieChartDataSettingsWithCustomUnits = (props: { name: string, fullPath: string, values: Array<any> }) => PieHoverTemplateSettings | {};

const usePieChartDataSettingsWithCustomUnits = ({ config }: { config: AggregationWidgetConfig }) => {
  const unitFeatureEnabled = useFeature(UNIT_FEATURE_FLAG);
  const widgetUnits = useWidgetUnits(config);

  return useCallback<PieChartDataSettingsWithCustomUnits>(({ name, fullPath, values }) => {
    if (!unitFeatureEnabled) return ({});

    const fieldNameKey = getFieldNameFromTrace({ fullPath, series: config.series }) ?? NO_FIELD_NAME_SERIES;
    const unit = widgetUnits.getFieldUnit(fieldNameKey);

    return ({
      fullPath,
      ...getPieHoverTemplateSettings({ convertedValues: values, unit, name }),
    });
  }, [config.series, unitFeatureEnabled, widgetUnits]);
};

export default usePieChartDataSettingsWithCustomUnits;
