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
import React, { useMemo, useCallback } from 'react';
import { useFormikContext } from 'formik';
import { useTheme } from 'styled-components';

import { Col } from 'components/bootstrap';
import { FormikInput } from 'components/common';
import type { CustomFieldComponentProps } from 'views/types';
import type { WidgetConfigFormValues } from 'views/components/aggregationwizard';
import ColorConfigurationPopover from 'views/components/aggregationwizard/ColorConfigurationPopover';
import { getDefaultLabelColor } from 'views/components/visualizations/utils/getDefaultPlotFontSettings';
import type { ChartAxisConfig } from 'views/logic/aggregationbuilder/visualizations/XYVisualization';

const AxisVisualizationField = ({ name, field, title, inputHelp }: CustomFieldComponentProps) => {
  const theme = useTheme();
  const { values, setFieldValue } = useFormikContext<WidgetConfigFormValues>();

  const curColor = useMemo(() => {
    const visualizationConfig = values.visualization.config;
    const defaultColor = getDefaultLabelColor(theme);
    if ('axisConfig' in visualizationConfig) {
      return visualizationConfig?.axisConfig?.[field.id]?.color ?? defaultColor;
    }

    return defaultColor;
  }, [field.id, theme, values.visualization.config]);

  const onColorSelect = useCallback(
    (color: string) => {
      setFieldValue(`${name}.color`, color);
    },
    [name, setFieldValue],
  );

  const validateField = useCallback(
    (value: string) => {
      const axisConfig: ChartAxisConfig =
        'axisConfig' in values.visualization.config ? values.visualization.config.axisConfig : {};
      const hasError =
        'showAxisLabels' in values.visualization.config &&
        values.visualization.config.showAxisLabels &&
        !Object.values(axisConfig).some(({ color, title: axisTitle }) => color ?? axisTitle) &&
        !value;

      if (hasError) return 'At least for one axis should be defined label or color.';

      return null;
    },
    [values.visualization.config],
  );

  return (
    <>
      <FormikInput
        id={`${name}.title-input`}
        label={title}
        bsSize="small"
        placeholder={field.description}
        name={`${name}.title`}
        labelClassName="col-sm-3"
        wrapperClassName="col-sm-8"
        formGroupClassName=""
        help={inputHelp}
        validate={validateField}
      />
      <Col sm={1}>
        <ColorConfigurationPopover
          title={`Color configuration for ${field.title}`}
          curColor={curColor}
          onColorSelect={onColorSelect}
        />
      </Col>
    </>
  );
};

export default AxisVisualizationField;
