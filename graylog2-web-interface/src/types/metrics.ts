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
export type CounterMetric = {
  metric: {
    count: number;
  };
  type: 'counter';
};

export type GaugeMetric = {
  metric: {
    value: number;
  };
  type: 'gauge';
};

export type Rate = {
  rate: {
    total: number;
    mean: number;
    one_minute: number;
    five_minute: number;
    fifteen_minute: number;
  };
  rate_unit: string;
};

export type MeterMetric = {
  metric: Rate;
  type: 'meter';
};

type Timing = {
  '95th_percentile': number;
  '98th_percentile': number;
  '99th_percentile': number;
  'std_dev': number;
  mean: number;
  min: number;
  max: number;
};

export type TimerMetric = {
  metric: Rate & {
    time: Timing;
  };
  type: 'timer';
};

export type HistogramMetric = {
  metric: {
    time: Timing;
    count: number;
  };
  type: 'histogram';
};

type BaseMetric<T> = {
  full_name: string;
  name: string;
} & T;

export type Metric =
  | BaseMetric<CounterMetric>
  | BaseMetric<GaugeMetric>
  | BaseMetric<MeterMetric>
  | BaseMetric<TimerMetric>
  | BaseMetric<HistogramMetric>;

export type NodeMetric = {
  [metricName: string]: Metric;
};

export type ClusterMetric = {
  [nodeId: string]: NodeMetric;
};
