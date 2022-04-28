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
import type { Rows } from 'views/logic/searchtypes/pivot/PivotHandler';

type Fixture = { [key: string]: Rows };
export const oneRowPivotOneColumnPivot: Fixture = {
  chart: [
    {
      key: [
        'index',
      ],
      values: [
        {
          key: [
            'PostsController',
            'count()',
          ],
          value: 6863,
          rollup: false,
          source: 'col-leaf',
        },
        {
          key: [
            'UsersController',
            'count()',
          ],
          value: 475,
          rollup: false,
          source: 'col-leaf',
        },
        {
          key: [
            'count()',
          ],
          value: 7338,
          rollup: true,
          source: 'row-leaf',
        },
      ],
      source: 'leaf',
    },
    {
      key: [
        'show',
      ],
      values: [
        {
          key: [
            'PostsController',
            'count()',
          ],
          value: 2276,
          rollup: false,
          source: 'col-leaf',
        },
        {
          key: [
            'count()',
          ],
          value: 2276,
          rollup: true,
          source: 'row-leaf',
        },
      ],
      source: 'leaf',
    },
    {
      key: [
        'login',
      ],
      values: [
        {
          key: [
            'LoginController',
            'count()',
          ],
          value: 1925,
          rollup: false,
          source: 'col-leaf',
        },
        {
          key: [
            'count()',
          ],
          value: 1925,
          rollup: true,
          source: 'row-leaf',
        },
      ],
      source: 'leaf',
    },
    {
      key: [
        'edit',
      ],
      values: [
        {
          key: [
            'PostsController',
            'count()',
          ],
          value: 340,
          rollup: false,
          source: 'col-leaf',
        },
        {
          key: [
            'count()',
          ],
          value: 340,
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
            'count()',
          ],
          value: 11879,
          rollup: true,
          source: 'row-inner',
        },
      ],
      source: 'non-leaf',
    },
  ],
};

export const oneRowPivot: Fixture = {
  chart: [
    {
      key: [
        'index',
      ],
      values: [
        {
          key: [
            'count()',
          ],
          value: 7482,
          rollup: true,
          source: 'row-leaf',
        },
      ],
      source: 'leaf',
    },
    {
      key: [
        'show',
      ],
      values: [
        {
          key: [
            'count()',
          ],
          value: 2211,
          rollup: true,
          source: 'row-leaf',
        },
      ],
      source: 'leaf',
    },
    {
      key: [
        'login',
      ],
      values: [
        {
          key: [
            'count()',
          ],
          value: 1865,
          rollup: true,
          source: 'row-leaf',
        },
      ],
      source: 'leaf',
    },
    {
      key: [
        'edit',
      ],
      values: [
        {
          key: [
            'count()',
          ],
          value: 323,
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
            'count()',
          ],
          value: 11881,
          rollup: true,
          source: 'row-inner',
        },
      ],
      source: 'non-leaf',
    },
  ],
};
