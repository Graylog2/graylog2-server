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
export const simpleChartData = {
  chart: [
    {
      key: [
        '2019-11-28T15:21:00.000Z',
      ],
      values: [
        {
          key: [
            'avg(nf_bytes)',
          ],
          value: 24558.239393939395,
          rollup: true,
          source: 'row-leaf',
        },
        {
          key: [
            'sum(nf_pkts)',
          ],
          value: 14967,
          rollup: true,
          source: 'row-leaf',
        },
      ],
      source: 'leaf',
    },
    {
      key: [
        '2019-11-28T15:22:00.000Z',
      ],
      values: [
        {
          key: [
            'avg(nf_bytes)',
          ],
          value: 3660.5666666666666,
          rollup: true,
          source: 'row-leaf',
        },
        {
          key: [
            'sum(nf_pkts)',
          ],
          value: 1239,
          rollup: true,
          source: 'row-leaf',
        },
      ],
      source: 'leaf',
    },
    {
      key: [
        '2019-11-28T15:23:00.000Z',
      ],
      values: [
        {
          key: [
            'avg(nf_bytes)',
          ],
          value: 49989.69,
          rollup: true,
          source: 'row-leaf',
        },
        {
          key: [
            'sum(nf_pkts)',
          ],
          value: 20776,
          rollup: true,
          source: 'row-leaf',
        },
      ],
      source: 'leaf',
    },
    {
      key: [
        '2019-11-28T15:24:00.000Z',
      ],
      values: [
        {
          key: [
            'avg(nf_bytes)',
          ],
          value: 2475.225,
          rollup: true,
          source: 'row-leaf',
        },
        {
          key: [
            'sum(nf_pkts)',
          ],
          value: 1285,
          rollup: true,
          source: 'row-leaf',
        },
      ],
      source: 'leaf',
    },
    {
      key: [
        '2019-11-28T15:25:00.000Z',
      ],
      values: [
        {
          key: [
            'avg(nf_bytes)',
          ],
          value: 10034.822222222223,
          rollup: true,
          source: 'row-leaf',
        },
        {
          key: [
            'sum(nf_pkts)',
          ],
          value: 4377,
          rollup: true,
          source: 'row-leaf',
        },
      ],
      source: 'leaf',
    },
    {
      key: [],
      values: [
        {
          key: [
            'avg(nf_bytes)',
          ],
          value: 25033.207843137254,
          rollup: true,
          source: 'row-inner',
        },
        {
          key: [
            'sum(nf_pkts)',
          ],
          value: 42644,
          rollup: true,
          source: 'row-inner',
        },
      ],
      source: 'non-leaf',
    },
  ],
};

export const effectiveTimerange = {
  type: 'absolute',
  from: '2019-11-28T15:21:00.486Z',
  to: '2019-11-28T15:25:57.000Z',
};
