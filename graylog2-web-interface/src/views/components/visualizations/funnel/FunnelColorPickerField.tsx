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
import React, { useCallback } from 'react';
import { useField } from 'formik';
import styled from 'styled-components';

import type { CustomFieldComponentProps } from 'views/types';
import ColorConfigurationPopover from 'views/components/aggregationwizard/ColorConfigurationPopover';
import { DEFAULT_FUNNEL_START_COLOR } from 'views/logic/aggregationbuilder/visualizations/FunnelVisualizationConfig';

const Row = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
`;

const FunnelColorPickerField = ({ name, title, field }: CustomFieldComponentProps) => {
  const [{ value }, , { setValue }] = useField<string>(name);
  const curColor = value ?? DEFAULT_FUNNEL_START_COLOR;

  const onColorSelect = useCallback(
    (color: string) => setValue(color),
    [setValue],
  );

  return (
    <Row>
      {title}
      <ColorConfigurationPopover title={field.title} curColor={curColor} onColorSelect={onColorSelect} />
    </Row>
  );
};

export default FunnelColorPickerField;
