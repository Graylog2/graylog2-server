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
import { readFileSync } from 'fs';
import { dirname } from 'path';

import Search from 'views/logic/search/Search';
import View from 'views/logic/views/View';

import copyWidgetToDashboard from './CopyWidgetToDashboard';

import ValueParameter from '../parameters/ValueParameter';
import Parameter from '../parameters/Parameter';

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

const cwd = dirname(__filename);
const readFixture = (filename) => JSON.parse(readFileSync(`${cwd}/${filename}`).toString());

describe('copyWidgetToDashboard', () => {
  beforeEach(() => {
    Parameter.registerSubtype(ValueParameter.type, ValueParameter);
  });

  const generateSearchView = () => {
    const searchViewFixture = View.fromJSON(readFixture('./CopyWidgetToDashboard.Search-View.fixture.json'));
    const searchSearchFixture = Search.fromJSON(readFixture('./CopyWidgetToDashboard.Search-Search.fixture.json'));

    return searchViewFixture.toBuilder()
      .search(searchSearchFixture)
      .build();
  };

  it('should copy a widget to a dashboard', () => {
    const searchView = generateSearchView();
    const dashboardViewFixture = View.fromJSON(readFixture('./CopyWidgetToDashboard.Dashboard-View.fixture.json'));
    const dashboardSearchFixture = Search.fromJSON(readFixture('./CopyWidgetToDashboard.Dashboard-Search.fixture.json'));
    const dashboardView = dashboardViewFixture.toBuilder()
      .search(dashboardSearchFixture)
      .build();

    const widgetId = '4d73ccaa-aabf-451a-b36e-309f55798e04';

    const newDashboard = copyWidgetToDashboard(widgetId, searchView, dashboardView);

    expect(newDashboard).toMatchSnapshot();
  });

  it('should copy a widget to first dashboard page, when dashboard has multiple pages', () => {
    // The fixtures view state queries order is different than the search queries order.
    // This test ensures that we are still determining the first dashboard page (query) correctly.
    const searchView = generateSearchView();
    const dashboardViewFixture = View.fromJSON(readFixture('./CopyWidgetToDashboard.Dashboard-View-Multiple-Queries.fixture.json'));
    const dashboardSearchFixture = Search.fromJSON(readFixture('./CopyWidgetToDashboard.Dashboard-Search-Multiple-Queries.fixture.json'));
    const dashboardView = dashboardViewFixture.toBuilder()
      .search(dashboardSearchFixture)
      .build();

    const widgetId = '4d73ccaa-aabf-451a-b36e-309f55798e04';

    const newDashboard = copyWidgetToDashboard(widgetId, searchView, dashboardView);
    const firstQueryId = newDashboard.search.queries.first().id;
    const widgetQueryId = newDashboard.state.findKey((query) => !!query.widgets.find(({ id }) => id === 'dead-beef'));

    expect(widgetQueryId).toBe(firstQueryId);
  });
});
