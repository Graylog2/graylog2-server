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
import { useCallback, useMemo } from 'react';

import useFeature from 'hooks/useFeature';
import useWidgetUnits from 'views/components/visualizations/hooks/useWidgetUnits';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import {
  generateMappersForYAxis, getHoverTemplateSettings,
} from 'views/components/visualizations/utils/chartLayoutGenerators';
import convertDataToBaseUnit from 'views/components/visualizations/utils/convertDataToBaseUnit';
import { NO_FIELD_NAME_SERIES, UNIT_FEATURE_FLAG } from 'views/components/visualizations/Constants';
import { getBaseUnit } from 'views/components/visualizations/utils/unitConverters';
import FieldUnit from 'views/logic/aggregationbuilder/FieldUnit';
import isLayoutRequiresBaseUnit from 'views/components/visualizations/utils/isLayoutRequiresBaseUnit';
import getFieldNameFromTrace from 'views/components/visualizations/utils/getFieldNameFromTrace';
import type { ChartDefinition } from 'views/components/visualizations/ChartData';

const useChartDataSettingsWithCustomUnits = ({ config }: {
  config: AggregationWidgetConfig,
}) => {
  const unitFeatureEnabled = useFeature(UNIT_FEATURE_FLAG);
  const widgetUnits = useWidgetUnits(config);
  const { fieldNameToAxisNameMapper } = useMemo(() => generateMappersForYAxis({ series: config.series, units: widgetUnits }), [config.series, widgetUnits]);

  return useCallback(({ name, values, fullPath }: { name: string, values: Array<any>, fullPath: string }): Partial<ChartDefinition> => {
    if (!unitFeatureEnabled) return ({});
    const fieldNameKey = getFieldNameFromTrace({ fullPath, series: config.series }) ?? NO_FIELD_NAME_SERIES;
    const yaxis = fieldNameToAxisNameMapper[fieldNameKey];
    const curUnit = widgetUnits.getFieldUnit(fieldNameKey);
    const shouldConvertToBaseUnit = isLayoutRequiresBaseUnit(curUnit);
    const convertedValues = shouldConvertToBaseUnit ? convertDataToBaseUnit(values, curUnit) : values;
    const baseUnit = shouldConvertToBaseUnit && getBaseUnit(curUnit?.unitType);
    const unit = shouldConvertToBaseUnit ? new FieldUnit(baseUnit?.unitType, baseUnit?.abbrev) : curUnit;

    return ({
      yaxis,
      y: convertedValues,
      fullPath,
      ...getHoverTemplateSettings({ unit, convertedValues, name }),
    });
  }, [config.series, fieldNameToAxisNameMapper, unitFeatureEnabled, widgetUnits]);
};

export default useChartDataSettingsWithCustomUnits;
