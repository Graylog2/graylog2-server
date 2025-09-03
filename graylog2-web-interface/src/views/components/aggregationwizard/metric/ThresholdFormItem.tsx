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
import React, { useMemo, useState } from 'react';
import styled from 'styled-components';
import { useFormikContext } from 'formik';
import { Button } from '@mantine/core';

import Popover from 'components/common/Popover';
import ColorPicker from 'components/common/ColorPicker';
import { colors as defaultColors } from 'views/components/visualizations/Colors';
import { Col } from 'components/bootstrap';
import { FormikInput } from 'components/common';
import type { WidgetConfigFormValues } from 'views/components/aggregationwizard';

const ColorHint = styled.div(
  ({ color }) => css`
    cursor: pointer;
    background-color: ${color} !important; /* Needed for report generation */
    -webkit-print-color-adjust: exact !important; /* Needed for report generation */
    width: 12px;
    height: 12px;
  `,
);

type Props = { metricIndex: number; thresholdIndex: number; remove: () => void };

const ThresholdFormItem = ({ metricIndex, remove, thresholdIndex }: Props) => {
  const {
    values: { metrics },
    setFieldValue,
  } = useFormikContext<WidgetConfigFormValues>();

  const [showPopover, setShowPopover] = useState(false);
  const togglePopover = () => setShowPopover((show) => !show);

  const _onColorSelect = (color: string) =>
    setFieldValue(`metrics.${metricIndex}.thresholds.${thresholdIndex}.name`, color);

  const curColor = useMemo(
    () => metrics?.[metricIndex]?.thresholds?.[thresholdIndex]?.color,
    [metricIndex, metrics, thresholdIndex],
  );

  return (
    <>
      <Col sm={11}>
        <FormikInput
          key={`metrics-${metricIndex}-thresholds-${thresholdIndex}-name`}
          id="thresholdName"
          label="Name"
          bsSize="small"
          placeholder="Specify threshold name"
          name={`metrics.${metricIndex}.thresholds.${thresholdIndex}.name`}
          labelClassName="col-sm-3"
          wrapperClassName="col-sm-9"
        />
      </Col>
      <Col sm={3}>
        <Popover position="top" withArrow opened={showPopover}>
          <Popover.Target>
            <ColorHint aria-label="Color Hint" onClick={togglePopover} color={curColor} />
          </Popover.Target>
          <Popover.Dropdown title="Color configuration for threshold">
            <ColorPicker color={curColor} colors={defaultColors} onChange={_onColorSelect} />
          </Popover.Dropdown>
        </Popover>
      </Col>
      <Col sm={8}>
        <FormikInput
          key={`metrics-${metricIndex}-thresholds-${thresholdIndex}-value`}
          id="thresholdValue"
          bsSize="small"
          placeholder="Specify threshold value"
          name={`metrics.${metricIndex}.thresholds.${thresholdIndex}.value`}
          wrapperClassName="col-sm-7"
        />
      </Col>
      <Button onClick={remove} bsSize="xs">
        Remove
      </Button>
    </>
  );
};

export default ThresholdFormItem;
