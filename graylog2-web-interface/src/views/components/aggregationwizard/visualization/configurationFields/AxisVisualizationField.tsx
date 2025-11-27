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
import get from 'lodash/get';

import { Col } from 'components/bootstrap';
import { FormikInput } from 'components/common';
import type { CustomFieldComponentProps } from 'views/types';
import type { WidgetConfigFormValues } from 'views/components/aggregationwizard';
import ColorConfigurationPopover from 'views/components/aggregationwizard/ColorConfigurationPopover';
import { getDefaultLabelColor } from 'views/components/visualizations/utils/getDefaultPlotFontSettings';

const AxisVisualizationField = ({ name, field, title, inputHelp }: CustomFieldComponentProps) => {
  const theme = useTheme();
  const { values, setFieldValue } = useFormikContext<WidgetConfigFormValues>();

  const curColor = useMemo(
    () => get(values.visualization.config, `${field.name}.color`) ?? getDefaultLabelColor(theme),
    [field.name, theme, values.visualization.config],
  );

  const onColorSelect = useCallback(
    (color: string) => {
      setFieldValue(`${name}.color`, color);
    },
    [name, setFieldValue],
  );

  return (
    <>
      <Col sm={11}>
        <FormikInput
          id={`${name}.title-input`}
          label={title}
          bsSize="small"
          placeholder={field.description}
          name={`${name}.title`}
          labelClassName="col-sm-3"
          wrapperClassName="col-sm-9"
          help={inputHelp}
        />
      </Col>
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
