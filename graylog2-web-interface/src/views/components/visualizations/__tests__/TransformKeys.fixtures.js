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
export const singleRowPivot = {
  rowPivots: [{ field: 'timestamp', type: 'time', config: { interval: { type: 'auto' } } }],
  columnPivots: [],
  input: [
    {
      key: ['2018-10-02T11:12:30.000Z'],
      values: [{ key: ['count()'], value: 222, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    }, {
      key: ['2018-10-02T11:12:40.000Z'],
      values: [{ key: ['count()'], value: 0, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    }, {
      key: ['2018-10-02T11:12:50.000Z'],
      values: [{ key: ['count()'], value: 0, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    }, {
      key: ['2018-10-02T11:13:00.000Z'],
      values: [{ key: ['count()'], value: 0, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    }, {
      key: ['2018-10-02T11:13:10.000Z'],
      values: [{ key: ['count()'], value: 0, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    }, {
      key: ['2018-10-02T11:13:20.000Z'],
      values: [{ key: ['count()'], value: 0, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    }, {
      key: ['2018-10-02T11:13:30.000Z'],
      values: [{ key: ['count()'], value: 0, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    }, {
      key: ['2018-10-02T11:13:40.000Z'],
      values: [{ key: ['count()'], value: 922, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    }, {
      key: ['2018-10-02T11:13:50.000Z'],
      values: [{ key: ['count()'], value: 1244, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    }, {
      key: ['2018-10-02T11:14:00.000Z'],
      values: [{ key: ['count()'], value: 1316, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    }, {
      key: ['2018-10-02T11:14:10.000Z'],
      values: [{ key: ['count()'], value: 1788, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    }, {
      key: ['2018-10-02T11:14:20.000Z'],
      values: [{ key: ['count()'], value: 1383, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    }, {
      key: ['2018-10-02T11:14:30.000Z'],
      values: [{ key: ['count()'], value: 1228, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    }, {
      key: ['2018-10-02T11:14:40.000Z'],
      values: [{ key: ['count()'], value: 142, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    }, {
      key: ['2018-10-02T11:14:50.000Z'],
      values: [{ key: ['count()'], value: 445, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    }, {
      key: ['2018-10-02T11:15:00.000Z'],
      values: [{ key: ['count()'], value: 1558, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    }, {
      key: ['2018-10-02T11:15:10.000Z'],
      values: [{ key: ['count()'], value: 1542, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    }, {
      key: ['2018-10-02T11:15:20.000Z'],
      values: [{ key: ['count()'], value: 1735, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    }, {
      key: ['2018-10-02T11:15:30.000Z'],
      values: [{ key: ['count()'], value: 1802, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    }, {
      key: ['2018-10-02T11:15:40.000Z'],
      values: [{ key: ['count()'], value: 1635, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    }, {
      key: ['2018-10-02T11:15:50.000Z'],
      values: [{ key: ['count()'], value: 1556, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    }, {
      key: ['2018-10-02T11:16:00.000Z'],
      values: [{ key: ['count()'], value: 1563, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    }, {
      key: ['2018-10-02T11:16:10.000Z'],
      values: [{ key: ['count()'], value: 1985, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    }, {
      key: ['2018-10-02T11:16:20.000Z'],
      values: [{ key: ['count()'], value: 1525, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    }, {
      key: ['2018-10-02T11:16:30.000Z'],
      values: [{ key: ['count()'], value: 1550, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    }, {
      key: ['2018-10-02T11:16:40.000Z'],
      values: [{ key: ['count()'], value: 1606, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    }, {
      key: ['2018-10-02T11:16:50.000Z'],
      values: [{ key: ['count()'], value: 1442, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    }, {
      key: ['2018-10-02T11:17:00.000Z'],
      values: [{ key: ['count()'], value: 76, rollup: true, source: 'row-leaf' }],
      source: 'leaf',
    }, {
      key: [],
      values: [{ key: ['count()'], value: 28265, rollup: true, source: 'row-inner' }],
      source: 'non-leaf',
    }],
  output: [
    {
      key: ['2018-10-02T07:12:30.000-04:00'],
      source: 'leaf',
      values: [{ key: ['count()'], rollup: true, source: 'row-leaf', value: 222 }],
    }, {
      key: ['2018-10-02T07:12:40.000-04:00'],
      source: 'leaf',
      values: [{ key: ['count()'], rollup: true, source: 'row-leaf', value: 0 }],
    }, {
      key: ['2018-10-02T07:12:50.000-04:00'],
      source: 'leaf',
      values: [{ key: ['count()'], rollup: true, source: 'row-leaf', value: 0 }],
    }, {
      key: ['2018-10-02T07:13:00.000-04:00'],
      source: 'leaf',
      values: [{ key: ['count()'], rollup: true, source: 'row-leaf', value: 0 }],
    }, {
      key: ['2018-10-02T07:13:10.000-04:00'],
      source: 'leaf',
      values: [{ key: ['count()'], rollup: true, source: 'row-leaf', value: 0 }],
    }, {
      key: ['2018-10-02T07:13:20.000-04:00'],
      source: 'leaf',
      values: [{ key: ['count()'], rollup: true, source: 'row-leaf', value: 0 }],
    }, {
      key: ['2018-10-02T07:13:30.000-04:00'],
      source: 'leaf',
      values: [{ key: ['count()'], rollup: true, source: 'row-leaf', value: 0 }],
    }, {
      key: ['2018-10-02T07:13:40.000-04:00'],
      source: 'leaf',
      values: [{ key: ['count()'], rollup: true, source: 'row-leaf', value: 922 }],
    }, {
      key: ['2018-10-02T07:13:50.000-04:00'],
      source: 'leaf',
      values: [{ key: ['count()'], rollup: true, source: 'row-leaf', value: 1244 }],
    }, {
      key: ['2018-10-02T07:14:00.000-04:00'],
      source: 'leaf',
      values: [{ key: ['count()'], rollup: true, source: 'row-leaf', value: 1316 }],
    }, {
      key: ['2018-10-02T07:14:10.000-04:00'],
      source: 'leaf',
      values: [{ key: ['count()'], rollup: true, source: 'row-leaf', value: 1788 }],
    }, {
      key: ['2018-10-02T07:14:20.000-04:00'],
      source: 'leaf',
      values: [{ key: ['count()'], rollup: true, source: 'row-leaf', value: 1383 }],
    }, {
      key: ['2018-10-02T07:14:30.000-04:00'],
      source: 'leaf',
      values: [{ key: ['count()'], rollup: true, source: 'row-leaf', value: 1228 }],
    }, {
      key: ['2018-10-02T07:14:40.000-04:00'],
      source: 'leaf',
      values: [{ key: ['count()'], rollup: true, source: 'row-leaf', value: 142 }],
    }, {
      key: ['2018-10-02T07:14:50.000-04:00'],
      source: 'leaf',
      values: [{ key: ['count()'], rollup: true, source: 'row-leaf', value: 445 }],
    }, {
      key: ['2018-10-02T07:15:00.000-04:00'],
      source: 'leaf',
      values: [{ key: ['count()'], rollup: true, source: 'row-leaf', value: 1558 }],
    }, {
      key: ['2018-10-02T07:15:10.000-04:00'],
      source: 'leaf',
      values: [{ key: ['count()'], rollup: true, source: 'row-leaf', value: 1542 }],
    }, {
      key: ['2018-10-02T07:15:20.000-04:00'],
      source: 'leaf',
      values: [{ key: ['count()'], rollup: true, source: 'row-leaf', value: 1735 }],
    }, {
      key: ['2018-10-02T07:15:30.000-04:00'],
      source: 'leaf',
      values: [{ key: ['count()'], rollup: true, source: 'row-leaf', value: 1802 }],
    }, {
      key: ['2018-10-02T07:15:40.000-04:00'],
      source: 'leaf',
      values: [{ key: ['count()'], rollup: true, source: 'row-leaf', value: 1635 }],
    }, {
      key: ['2018-10-02T07:15:50.000-04:00'],
      source: 'leaf',
      values: [{ key: ['count()'], rollup: true, source: 'row-leaf', value: 1556 }],
    }, {
      key: ['2018-10-02T07:16:00.000-04:00'],
      source: 'leaf',
      values: [{ key: ['count()'], rollup: true, source: 'row-leaf', value: 1563 }],
    }, {
      key: ['2018-10-02T07:16:10.000-04:00'],
      source: 'leaf',
      values: [{ key: ['count()'], rollup: true, source: 'row-leaf', value: 1985 }],
    }, {
      key: ['2018-10-02T07:16:20.000-04:00'],
      source: 'leaf',
      values: [{ key: ['count()'], rollup: true, source: 'row-leaf', value: 1525 }],
    }, {
      key: ['2018-10-02T07:16:30.000-04:00'],
      source: 'leaf',
      values: [{ key: ['count()'], rollup: true, source: 'row-leaf', value: 1550 }],
    }, {
      key: ['2018-10-02T07:16:40.000-04:00'],
      source: 'leaf',
      values: [{ key: ['count()'], rollup: true, source: 'row-leaf', value: 1606 }],
    }, {
      key: ['2018-10-02T07:16:50.000-04:00'],
      source: 'leaf',
      values: [{ key: ['count()'], rollup: true, source: 'row-leaf', value: 1442 }],
    }, {
      key: ['2018-10-02T07:17:00.000-04:00'],
      source: 'leaf',
      values: [{ key: ['count()'], rollup: true, source: 'row-leaf', value: 76 }],
    }, {
      key: [],
      source: 'non-leaf',
      values: [{ key: ['count()'], rollup: true, source: 'row-inner', value: 28265 }],
    }],
};

export const noTimePivots = {
  rowPivots: [{ field: 'http_method', type: 'values', config: { limit: 15 } }],
  columnPivots: [
    { field: 'action', type: 'values', config: { limit: 15 } },
    {
      field: 'controller',
      type: 'values',
      config: { limit: 15 },
    },
  ],
  input: [
    {
      key: ['GET'],
      values: [{
        key: ['index', 'PostsController', 'count()'],
        value: 21750,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['index', 'UsersController', 'count()'],
        value: 1532,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['index', 'count()'],
        value: 23282,
        rollup: true,
        source: 'col-inner',
      }, {
        key: ['show', 'PostsController', 'count()'],
        value: 6798,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['show', 'count()'],
        value: 6798,
        rollup: true,
        source: 'col-inner',
      }, {
        key: ['login', 'LoginController', 'count()'],
        value: 6018,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['login', 'count()'],
        value: 6018,
        rollup: true,
        source: 'col-inner',
      }, {
        key: ['edit', 'PostsController', 'count()'],
        value: 1040,
        rollup: false,
        source: 'col-leaf',
      }, { key: ['edit', 'count()'], value: 1040, rollup: true, source: 'col-inner' }, {
        key: ['count()'],
        value: 37138,
        rollup: true,
        source: 'row-leaf',
      }],
      source: 'leaf',
    }, {
      key: ['POST'],
      values: [{
        key: ['index', 'PostsController', 'count()'],
        value: 1220,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['index', 'UsersController', 'count()'],
        value: 112,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['index', 'count()'],
        value: 1332,
        rollup: true,
        source: 'col-inner',
      }, {
        key: ['show', 'PostsController', 'count()'],
        value: 424,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['show', 'count()'],
        value: 424,
        rollup: true,
        source: 'col-inner',
      }, {
        key: ['login', 'LoginController', 'count()'],
        value: 354,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['login', 'count()'],
        value: 354,
        rollup: true,
        source: 'col-inner',
      }, {
        key: ['edit', 'PostsController', 'count()'],
        value: 34,
        rollup: false,
        source: 'col-leaf',
      }, { key: ['edit', 'count()'], value: 34, rollup: true, source: 'col-inner' }, {
        key: ['count()'],
        value: 2144,
        rollup: true,
        source: 'row-leaf',
      }],
      source: 'leaf',
    }, {
      key: ['DELETE'],
      values: [{
        key: ['index', 'PostsController', 'count()'],
        value: 1232,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['index', 'UsersController', 'count()'],
        value: 106,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['index', 'count()'],
        value: 1338,
        rollup: true,
        source: 'col-inner',
      }, {
        key: ['show', 'PostsController', 'count()'],
        value: 382,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['show', 'count()'],
        value: 382,
        rollup: true,
        source: 'col-inner',
      }, {
        key: ['login', 'LoginController', 'count()'],
        value: 312,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['login', 'count()'],
        value: 312,
        rollup: true,
        source: 'col-inner',
      }, {
        key: ['edit', 'PostsController', 'count()'],
        value: 58,
        rollup: false,
        source: 'col-leaf',
      }, { key: ['edit', 'count()'], value: 58, rollup: true, source: 'col-inner' }, {
        key: ['count()'],
        value: 2090,
        rollup: true,
        source: 'row-leaf',
      }],
      source: 'leaf',
    }, {
      key: ['PUT'],
      values: [{
        key: ['index', 'PostsController', 'count()'],
        value: 990,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['index', 'UsersController', 'count()'],
        value: 64,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['index', 'count()'],
        value: 1054,
        rollup: true,
        source: 'col-inner',
      }, {
        key: ['login', 'LoginController', 'count()'],
        value: 302,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['login', 'count()'],
        value: 302,
        rollup: true,
        source: 'col-inner',
      }, {
        key: ['show', 'PostsController', 'count()'],
        value: 290,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['show', 'count()'],
        value: 290,
        rollup: true,
        source: 'col-inner',
      }, {
        key: ['edit', 'PostsController', 'count()'],
        value: 40,
        rollup: false,
        source: 'col-leaf',
      }, { key: ['edit', 'count()'], value: 40, rollup: true, source: 'col-inner' }, {
        key: ['count()'],
        value: 1686,
        rollup: true,
        source: 'row-leaf',
      }],
      source: 'leaf',
    }, {
      key: [],
      values: [{ key: ['count()'], value: 45662, rollup: true, source: 'row-inner' }],
      source: 'non-leaf',
    },
  ],
  output: [
    {
      key: ['GET'],
      values: [{
        key: ['index', 'PostsController', 'count()'],
        value: 21750,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['index', 'UsersController', 'count()'],
        value: 1532,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['index', 'count()'],
        value: 23282,
        rollup: true,
        source: 'col-inner',
      }, {
        key: ['show', 'PostsController', 'count()'],
        value: 6798,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['show', 'count()'],
        value: 6798,
        rollup: true,
        source: 'col-inner',
      }, {
        key: ['login', 'LoginController', 'count()'],
        value: 6018,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['login', 'count()'],
        value: 6018,
        rollup: true,
        source: 'col-inner',
      }, {
        key: ['edit', 'PostsController', 'count()'],
        value: 1040,
        rollup: false,
        source: 'col-leaf',
      }, { key: ['edit', 'count()'], value: 1040, rollup: true, source: 'col-inner' }, {
        key: ['count()'],
        value: 37138,
        rollup: true,
        source: 'row-leaf',
      }],
      source: 'leaf',
    }, {
      key: ['POST'],
      values: [{
        key: ['index', 'PostsController', 'count()'],
        value: 1220,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['index', 'UsersController', 'count()'],
        value: 112,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['index', 'count()'],
        value: 1332,
        rollup: true,
        source: 'col-inner',
      }, {
        key: ['show', 'PostsController', 'count()'],
        value: 424,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['show', 'count()'],
        value: 424,
        rollup: true,
        source: 'col-inner',
      }, {
        key: ['login', 'LoginController', 'count()'],
        value: 354,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['login', 'count()'],
        value: 354,
        rollup: true,
        source: 'col-inner',
      }, {
        key: ['edit', 'PostsController', 'count()'],
        value: 34,
        rollup: false,
        source: 'col-leaf',
      }, { key: ['edit', 'count()'], value: 34, rollup: true, source: 'col-inner' }, {
        key: ['count()'],
        value: 2144,
        rollup: true,
        source: 'row-leaf',
      }],
      source: 'leaf',
    }, {
      key: ['DELETE'],
      values: [{
        key: ['index', 'PostsController', 'count()'],
        value: 1232,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['index', 'UsersController', 'count()'],
        value: 106,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['index', 'count()'],
        value: 1338,
        rollup: true,
        source: 'col-inner',
      }, {
        key: ['show', 'PostsController', 'count()'],
        value: 382,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['show', 'count()'],
        value: 382,
        rollup: true,
        source: 'col-inner',
      }, {
        key: ['login', 'LoginController', 'count()'],
        value: 312,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['login', 'count()'],
        value: 312,
        rollup: true,
        source: 'col-inner',
      }, {
        key: ['edit', 'PostsController', 'count()'],
        value: 58,
        rollup: false,
        source: 'col-leaf',
      }, { key: ['edit', 'count()'], value: 58, rollup: true, source: 'col-inner' }, {
        key: ['count()'],
        value: 2090,
        rollup: true,
        source: 'row-leaf',
      }],
      source: 'leaf',
    }, {
      key: ['PUT'],
      values: [{
        key: ['index', 'PostsController', 'count()'],
        value: 990,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['index', 'UsersController', 'count()'],
        value: 64,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['index', 'count()'],
        value: 1054,
        rollup: true,
        source: 'col-inner',
      }, {
        key: ['login', 'LoginController', 'count()'],
        value: 302,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['login', 'count()'],
        value: 302,
        rollup: true,
        source: 'col-inner',
      }, {
        key: ['show', 'PostsController', 'count()'],
        value: 290,
        rollup: false,
        source: 'col-leaf',
      }, {
        key: ['show', 'count()'],
        value: 290,
        rollup: true,
        source: 'col-inner',
      }, {
        key: ['edit', 'PostsController', 'count()'],
        value: 40,
        rollup: false,
        source: 'col-leaf',
      }, { key: ['edit', 'count()'], value: 40, rollup: true, source: 'col-inner' }, {
        key: ['count()'],
        value: 1686,
        rollup: true,
        source: 'row-leaf',
      }],
      source: 'leaf',
    }, {
      key: [],
      values: [{ key: ['count()'], value: 45662, rollup: true, source: 'row-inner' }],
      source: 'non-leaf',
    },
  ],
};
