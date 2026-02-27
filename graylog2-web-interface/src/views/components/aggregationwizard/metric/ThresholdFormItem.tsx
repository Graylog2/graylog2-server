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
import React, { useMemo } from 'react';
import { useFormikContext } from 'formik';

import { Col } from 'components/bootstrap';
import { FormikInput, IconButton } from 'components/common';
import type { WidgetConfigFormValues } from 'views/components/aggregationwizard';
import { mappedUnitsFromJSON } from 'views/components/visualizations/utils/unitConverters';
import ColorConfigurationPopover from 'views/components/aggregationwizard/ColorConfigurationPopover';

type Props = { metricIndex: number; thresholdIndex: number; onRemove: () => void };

const ThresholdFormItem = ({ metricIndex, thresholdIndex, onRemove }: Props) => {
  const {
    values: { metrics, units },
    setFieldValue,
  } = useFormikContext<WidgetConfigFormValues>();

  const onColorSelect = (color: string) =>
    setFieldValue(`metrics.${metricIndex}.thresholds.${thresholdIndex}.color`, color);

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
        <ColorConfigurationPopover
          onColorSelect={onColorSelect}
          curColor={curColor}
          title="Color configuration for threshold"
        />
      </Col>
    </>
  );
};

export default ThresholdFormItem;
