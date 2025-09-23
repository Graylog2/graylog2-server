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
import styled, { css } from 'styled-components';
import { useFormikContext } from 'formik';

import Popover from 'components/common/Popover';
import ColorPicker from 'components/common/ColorPicker';
import { colors as defaultColors } from 'views/components/visualizations/Colors';
import { Col } from 'components/bootstrap';
import { FormikInput, IconButton } from 'components/common';
import type { WidgetConfigFormValues } from 'views/components/aggregationwizard';
import { mappedUnitsFromJSON } from 'views/components/visualizations/utils/unitConverters';

const ColorHintWrapper = styled.div`
  width: 25px;
  height: 25px;
  display: flex;
  justify-content: center;
  align-items: center;
`;

const ColorHint = styled.div(
  ({ color, theme }) => css`
    cursor: pointer;
    background-color: ${color};
    width: ${theme.spacings.md};
    height: ${theme.spacings.md};
  `,
);

type Props = { metricIndex: number; thresholdIndex: number; onRemove: () => void };

const ThresholdFormItem = ({ metricIndex, thresholdIndex, onRemove }: Props) => {
  const {
    values: { metrics, units },
    setFieldValue,
  } = useFormikContext<WidgetConfigFormValues>();

  const [showPopover, setShowPopover] = useState(false);
  const togglePopover = () => setShowPopover((show) => !show);

  const _onColorSelect = (color: string) =>
    setFieldValue(`metrics.${metricIndex}.thresholds.${thresholdIndex}.color`, color).then(togglePopover);

  const curColor = useMemo(
    () => metrics?.[metricIndex]?.thresholds?.[thresholdIndex]?.color,
    [metricIndex, metrics, thresholdIndex],
  );

  const curUnitName = useMemo(() => {
    const field = metrics?.[metricIndex]?.field;
    const unit = units?.[field];
    if (!unit?.abbrev || !unit?.unitType) return null;

    return mappedUnitsFromJSON[unit.unitType].find(({ abbrev }) => abbrev === unit?.abbrev).name;
  }, [metricIndex, metrics, units]);

  return (
    <>
      <Col sm={11}>
        <FormikInput
          key={`metrics-${metricIndex}-thresholds-${thresholdIndex}-name`}
          id="thresholdName"
          label="Title"
          bsSize="small"
          placeholder="Specify threshold name"
          name={`metrics.${metricIndex}.thresholds.${thresholdIndex}.name`}
          labelClassName="col-sm-3"
          wrapperClassName="col-sm-9"
        />
      </Col>
      <Col sm={1}>
        <IconButton size="sm" onClick={onRemove} name="delete" title="Remove threshold" />
      </Col>
      <Col sm={11}>
        <FormikInput
          key={`metrics-${metricIndex}-thresholds-${thresholdIndex}-value`}
          id="thresholdValue"
          label="Value"
          bsSize="small"
          type="number"
          placeholder="Specify threshold value"
          name={`metrics.${metricIndex}.thresholds.${thresholdIndex}.value`}
          labelClassName="col-sm-3"
          wrapperClassName="col-sm-9"
          help={curUnitName && `Value is in ${curUnitName}s`}
        />
      </Col>
      <Col sm={1}>
        <Popover position="top" withArrow opened={showPopover}>
          <Popover.Target>
            <ColorHintWrapper>
              <ColorHint aria-label="Color Hint" onClick={togglePopover} color={curColor} />
            </ColorHintWrapper>
          </Popover.Target>
          <Popover.Dropdown title="Color configuration for threshold">
            <ColorPicker color={curColor} colors={defaultColors} onChange={_onColorSelect} />
          </Popover.Dropdown>
        </Popover>
      </Col>
    </>
  );
};

export default ThresholdFormItem;
