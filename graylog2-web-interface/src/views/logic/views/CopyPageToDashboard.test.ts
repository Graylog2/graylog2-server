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
import * as Immutable from 'immutable';

import Search from 'views/logic/search/Search';
import View from 'views/logic/views/View';
import readJsonFixture from 'helpers/readJsonFixture';

import copyPageToDashboard from './CopyPageToDashboard';

import ValueParameter from '../parameters/ValueParameter';
import Parameter from '../parameters/Parameter';

jest.useFakeTimers()
  .setSystemTime(1577836800000); // 2020-01-01 00:00:00.000

jest.mock('logic/generateId', () => jest.fn(() => 'dead-beef'));

jest.mock('bson-objectid', () => jest.fn(() => ({
  toString: jest.fn(() => 'new-search-id'),
})));

jest.mock('../Widgets', () => ({
  widgetDefinition: () => ({ searchTypes: () => [{ type: 'pivot' }] }),
}));

jest.mock('../SearchType', () => jest.fn(() => ({
  type: 'pivot',
  handler: jest.fn(),
  defaults: {},
})));

const targetDashboard = View.builder()
  .id('foo')
  .type(View.Type.Dashboard)
  .title('Foo')
  .summary('summary')
  .description('Foo')
  .search(Search.create().toBuilder().id('foosearch').build())
  .properties(Immutable.List())
  .state(Immutable.Map())
  .createdAt(new Date())
  .owner('admin')
  .requires({})
  .build();

const readFixture = (fixtureName: string) => readJsonFixture(__dirname, fixtureName);

describe('copyPageToDashboard', () => {
  beforeEach(() => {
    Parameter.registerSubtype(ValueParameter.type, ValueParameter);
  });

  it('should copy a page to a dashboard', () => {
    const dashboardViewFixture = View.fromJSON(readFixture('./CopyPageToDashboard.Dashboard-View.fixture.json'));
    const dashboardSearchFixture = Search.fromJSON(readFixture('./CopyPageToDashboard.Dashboard-Search.fixture.json'));
    const sourceDashboard = dashboardViewFixture.toBuilder()
      .search(dashboardSearchFixture)
      .build();

    const queryId = 'f0a1f93c-8400-40de-83ac-94149dbf447c';

    const newDashboard = copyPageToDashboard(queryId, sourceDashboard, targetDashboard);

    expect(newDashboard).toMatchSnapshot();
  });
});
