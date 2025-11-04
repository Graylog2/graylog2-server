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
import React, { useState, useEffect, useMemo } from 'react';

import type {
  ClickPoint,
  OnClickPopoverDropdown,
  FieldData,
  Step,
} from 'views/components/visualizations/OnClickPopover/Types';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import ClickPointSelector from 'views/components/visualizations/OnClickPopover/ClickPointSelector';
import ValueActionsDropdown from 'views/components/visualizations/OnClickPopover/ValueActionsDropdown';
import { AdditionalContext } from 'views/logic/ActionContext';
import useQueryFieldTypes from 'views/hooks/useQueryFieldTypes';

type Props = {
  component: OnClickPopoverDropdown;
  clickPoint: ClickPoint;
  config: AggregationWidgetConfig;
  clickPointsInRadius?: Array<ClickPoint>;
  metricMapper?: (clickPoint: ClickPoint) => { value: string; metric: string };
  onPopoverClose: () => void;
};

const defaultMetricMapper = (clickPoint: ClickPoint) => ({
  value: `${String(clickPoint.text ?? clickPoint.y)}`,
  metric: clickPoint.data.originalName ?? clickPoint.data.name,
});

const DropdownSwitcher = ({
  component: Component,
  clickPoint,
  config,
  clickPointsInRadius = [],
  metricMapper = defaultMetricMapper,
  onPopoverClose,
}: Props) => {
  const [step, setStep] = useState<Step>(null);
  const [selectedClickPoint, setSelectedClickPoint] = useState<ClickPoint>();
  const [fieldData, setFieldData] = useState<FieldData>(null);
  const types = useQueryFieldTypes();

  const onSelect = (pt: ClickPoint) => {
    setSelectedClickPoint(pt);
    setStep('values');
  };

  const hasPointsInRadius = useMemo(() => {
    const len = clickPointsInRadius?.length;

    return !!len && len > 1;
  }, [clickPointsInRadius]);

  useEffect(() => {
    setSelectedClickPoint(clickPoint);
    setStep(!hasPointsInRadius ? 'values' : 'traces');
    setFieldData(null);
  }, [clickPoint, clickPointsInRadius, hasPointsInRadius]);

  const additionalContextValue = useMemo(
    () => ({
      valuePath: fieldData?.contexts?.valuePath,
      fieldTypes: types,
    }),
    [fieldData?.contexts?.valuePath, types],
  );

  const onValueSelect = (data: FieldData) => {
    setStep('actions');
    setFieldData(data);
  };

  const onActionRun = () => {
    onPopoverClose();
    setFieldData(null);
    setStep(null);
  };

  if (!selectedClickPoint) return null;

  if (step === 'traces')
    return (
      <ClickPointSelector clickPointsInRadius={clickPointsInRadius} metricMapper={metricMapper} onSelect={onSelect} />
    );
  if (step === 'values')
    return (
      <Component
        clickPoint={selectedClickPoint}
        config={config}
        setFieldData={onValueSelect}
        showBackButton={hasPointsInRadius}
        setStep={setStep}
      />
    );
  if (step === 'actions')
    return (
      <AdditionalContext.Provider value={additionalContextValue}>
        <ValueActionsDropdown
          setStep={setStep}
          field={fieldData.field}
          value={fieldData.value}
          onActionRun={onActionRun}
        />
      </AdditionalContext.Provider>
    );

  return null;
};

export default DropdownSwitcher;
