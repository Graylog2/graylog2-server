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
import Immutable from 'immutable';
import moment from 'moment';
import { useTheme } from 'styled-components';

import EChart from 'views/components/visualizations/echarts/EChart';

const _formatTimestamp = (epoch: number) => moment.unix(epoch).format('YYYY-MM-DD HH:mm:ss');

type HistogramProps = {
  data: {
    config: {
      timerange: any;
    };
    interval: string;
    timerange: any;
    results: any;
  };
};

export default function Histogram({ data }: HistogramProps) {
  const theme = useTheme();

  const option = useMemo(() => {
    const ordered = Immutable.OrderedMap<string, number>(data.results);
    const xData = ordered
      .keySeq()
      .map((k) => _formatTimestamp(Number(k)))
      .toArray();
    const yData = ordered.valueSeq().toArray();

    return {
      animation: false,
      tooltip: {
        trigger: 'axis' as const,
        backgroundColor: theme.colors.global.contentBackground,
        borderColor: theme.colors.variant.light.default,
        textStyle: {
          color: theme.colors.text.primary,
          fontFamily: theme.fonts.family.body,
        },
      },
      xAxis: { type: 'category' as const, data: xData },
      yAxis: { type: 'value' as const },
      series: [{ type: 'bar' as const, name: 'took_ms', data: yData }],
      grid: { top: 10, left: 10, right: 10, bottom: 10, containLabel: true },
    };
  }, [data.results, theme]);

  return <EChart option={option} style={{ position: 'absolute', height: '100%', width: '100%' }} />;
}
