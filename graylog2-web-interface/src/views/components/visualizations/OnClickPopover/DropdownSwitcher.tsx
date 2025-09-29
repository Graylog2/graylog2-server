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
import React, { useState, useEffect, useMemo, useContext } from 'react';

import type {
  ClickPoint,
  OnClickPopoverDropdown,
  FieldData,
} from 'views/components/visualizations/OnClickPopover/Types';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import ClickPointSelector from 'views/components/visualizations/OnClickPopover/ClickPointSelector';
import ValueActionsDropdown from 'views/components/visualizations/OnClickPopover/ValueActionsDropdown';
import { AdditionalContext } from 'views/logic/ActionContext';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';

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

const useQueryFieldTypes = () => {
  const fieldTypes = useContext(FieldTypesContext);

  return useMemo(() => fieldTypes.currentQuery, [fieldTypes.currentQuery]);
};

const DropdownSwitcher = ({
  component: Component,
  clickPoint,
  config,
  clickPointsInRadius = [],
  metricMapper = defaultMetricMapper,
  onPopoverClose,
}: Props) => {
  const [selectedClickPoint, setSelectedClickPoint] = useState<ClickPoint>();
  const [showValuesComponent, setShowValuesComponent] = useState<boolean>();
  const [fieldData, setFieldData] = useState<FieldData>(null);
  const types = useQueryFieldTypes();

  const onSelect = (pt: ClickPoint) => {
    setShowValuesComponent(true);
    setSelectedClickPoint(pt);
  };

  useEffect(() => {
    setSelectedClickPoint(clickPoint);
    const len = clickPointsInRadius?.length;
    setShowValuesComponent(!len || len === 1);
    setFieldData(null);
  }, [clickPoint, clickPointsInRadius]);

  const additionalContextValue = useMemo(
    () => ({
      valuePath: fieldData?.contexts?.valuePath,
      fieldTypes: types,
    }),
    [fieldData?.contexts?.valuePath, types],
  );

  if (!selectedClickPoint) return null;

  const onActionRun = () => {
    onPopoverClose();
    setFieldData(null);
  };

  if (fieldData)
    return (
      <AdditionalContext.Provider value={additionalContextValue}>
        <ValueActionsDropdown field={fieldData.field} value={fieldData.value} onActionRun={onActionRun} />
      </AdditionalContext.Provider>
    );

  return showValuesComponent ? (
    <Component clickPoint={selectedClickPoint} config={config} setFieldData={setFieldData} />
  ) : (
    <ClickPointSelector clickPointsInRadius={clickPointsInRadius} metricMapper={metricMapper} onSelect={onSelect} />
  );
};

export default DropdownSwitcher;
