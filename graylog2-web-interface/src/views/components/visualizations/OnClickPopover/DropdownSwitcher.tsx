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
import React, { useState, useEffect } from 'react';

import type { ClickPoint } from 'views/components/visualizations/OnClickPopover/Types';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Popover from 'components/common/Popover';
import ClickPointSelector from 'views/components/visualizations/OnClickPopover/ClickPointSelector';

type Props = {
  component: React.ComponentType<{
    clickPoint: ClickPoint;
    config: AggregationWidgetConfig;
  }>;
  clickPoint: ClickPoint;
  config: AggregationWidgetConfig;
  clickPointsInRadius: Array<ClickPoint>;
  metricMapper?: (clickPoint: ClickPoint) => { value: string; metric: string };
};

const defaultMetricMapper = (clickPoint: ClickPoint) => ({
  value: `${String(clickPoint.text ?? clickPoint.y)}`,
  metric: clickPoint.data.originalName ?? clickPoint.data.name,
});

const DropdownSwitcher = ({
  component: Component,
  clickPoint,
  config,
  clickPointsInRadius,
  metricMapper = defaultMetricMapper,
}: Props) => {
  const [selectedClickPoint, setSelectedClickPoint] = useState<ClickPoint>();
  const [showComponent, setShowComponent] = useState<boolean>();

  const onSelect = (pt: ClickPoint) => {
    setShowComponent(true);
    setSelectedClickPoint(pt);
  };

  useEffect(() => {
    setSelectedClickPoint(clickPoint);
    const len = clickPointsInRadius?.length;
    setShowComponent(!len || len === 1);
  }, [clickPoint, clickPointsInRadius]);

  if (!selectedClickPoint) return null;

  return (
    <Popover.Dropdown>
      {showComponent ? (
        <Component clickPoint={selectedClickPoint} config={config} />
      ) : (
        <ClickPointSelector clickPointsInRadius={clickPointsInRadius} metricMapper={metricMapper} onSelect={onSelect} />
      )}
    </Popover.Dropdown>
  );
};

export default DropdownSwitcher;
