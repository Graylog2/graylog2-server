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
import React, { useRef, useEffect, useCallback } from 'react';
import ReactECharts from 'echarts-for-react';

import type { EChartsInstance } from 'views/components/visualizations/types';

type Props = {
  option: Record<string, any>;
  style?: React.CSSProperties;
  onEvents?: Record<string, (params: any) => void>;
  onChartReady?: (instance: EChartsInstance) => void;
};

const defaultStyle = { height: '100%', width: '100%' };

const EChart = ({ option, style = defaultStyle, onEvents, onChartReady }: Props) => {
  const chartRef = useRef<ReactECharts>(null);

  const handleChartReady = useCallback(
    (instance: EChartsInstance) => {
      onChartReady?.(instance);
    },
    [onChartReady],
  );

  useEffect(() => {
    const instance = chartRef.current?.getEchartsInstance();

    if (instance) {
      const resizeObserver = new ResizeObserver(() => {
        instance.resize();
      });
      const dom = instance.getDom();

      if (dom?.parentElement) {
        resizeObserver.observe(dom.parentElement);
      }

      return () => resizeObserver.disconnect();
    }

    return undefined;
  }, []);

  return (
    <ReactECharts
      ref={chartRef}
      option={option}
      style={style}
      notMerge
      lazyUpdate
      onEvents={onEvents}
      onChartReady={handleChartReady}
    />
  );
};

export default EChart;
