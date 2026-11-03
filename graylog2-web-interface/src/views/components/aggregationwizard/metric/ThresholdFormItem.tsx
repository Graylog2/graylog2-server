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
import { useTranslation } from 'react-i18next';
import { useFormikContext } from 'formik';

import { Col } from 'components/bootstrap';
import { FormikInput, IconButton } from 'components/common';
import type { WidgetConfigFormValues } from 'views/components/aggregationwizard';
import { mappedUnitsFromJSON } from 'views/components/visualizations/utils/unitConverters';
import { unitNameLabel } from 'views/components/visualizations/utils/unitLabels';
import ColorConfigurationPopover from 'views/components/aggregationwizard/ColorConfigurationPopover';

type Props = { metricIndex: number; thresholdIndex: number; onRemove: () => void };

const ThresholdFormItem = ({ metricIndex, thresholdIndex, onRemove }: Props) => {
  const { t } = useTranslation('aggregation');
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

    return unitNameLabel(mappedUnitsFromJSON[unit.unitType].find(({ abbrev }) => abbrev === unit?.abbrev).name);
  }, [metricIndex, metrics, units]);

  return (
    <>
      <Col sm={11}>
        <FormikInput
          key={`metrics-${metricIndex}-thresholds-${thresholdIndex}-name`}
          id="thresholdName"
          label={t('wizard_metric.threshold_title')}
          bsSize="small"
          placeholder={t('wizard_metric.threshold_name_placeholder')}
          name={`metrics.${metricIndex}.thresholds.${thresholdIndex}.name`}
          labelClassName="col-sm-3"
          wrapperClassName="col-sm-9"
        />
      </Col>
      <Col sm={1}>
        <IconButton size="sm" onClick={onRemove} name="delete" title={t('wizard_metric.remove_threshold')} />
      </Col>
      <Col sm={11}>
        <FormikInput
          key={`metrics-${metricIndex}-thresholds-${thresholdIndex}-value`}
          id="thresholdValue"
          label={t('wizard_metric.threshold_value')}
          bsSize="small"
          type="number"
          placeholder={t('wizard_metric.threshold_value_placeholder')}
          name={`metrics.${metricIndex}.thresholds.${thresholdIndex}.value`}
          labelClassName="col-sm-3"
          wrapperClassName="col-sm-9"
          help={curUnitName && t('wizard_metric.value_unit', { unit: curUnitName })}
        />
      </Col>
      <Col sm={1}>
        <ColorConfigurationPopover
          onColorSelect={onColorSelect}
          curColor={curColor}
          title={t('wizard_metric.threshold_color')}
        />
      </Col>
    </>
  );
};

export default ThresholdFormItem;
