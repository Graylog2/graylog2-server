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

import * as Immutable from 'immutable';
import { dirname } from 'path';

import MoveWidgetToTab from './MoveWidgetToTab';
import View from './View';

import Parameter from '../parameters/Parameter';
import ValueParameter from '../parameters/ValueParameter';
import Search from '../search/Search';

jest.mock('uuid/v4', () => jest.fn(() => 'dead-beef'));

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

jest.mock('views/logic/Widgets', () => ({
  widgetDefinition: jest.fn(() => ({
    defaultHeight: 4,
    defaultWidth: 4,
    type: 'aggregation',
    searchTypes: jest.fn(() => ([{}])),
  })),
}));

const cwd = dirname(__filename);
const readFixture = (filename) => JSON.parse(readFileSync(`${cwd}/${filename}`).toString());

const dashboardFixture = View.fromJSON(readFixture('./MoveWidgetToTab.Dashboard.fixture.json'));
const searchFixture = Search.fromJSON(readFixture('./MoveWidgetToTab.Search.fixture.json'));
const dashboard = dashboardFixture.toBuilder()
  .search(searchFixture)
  .build();

const widgetId = 'b34c3c6f-c49d-41d3-a65a-f746134f8f3e';
const targetQueryId = '5faea09b-4187-4eda-9d59-7a86d4774c73';
const sourceQueryId = 'fa247d8f-7afa-45b7-b57e-3cdef4ee53be';

describe('MoveWidgetToTab', () => {
  beforeEach(() => {
    Parameter.registerSubtype(ValueParameter.type, ValueParameter);
  });

  it('should move a Widget to a dashboard', () => {
    const newDashboard = MoveWidgetToTab(widgetId, targetQueryId, dashboard, false);

    expect(newDashboard).toMatchSnapshot();
  });

  it('should work when titles are empty', () => {
    const newStates = dashboard.state.update(
      sourceQueryId,
      (query) => query.toBuilder().titles(Immutable.Map()).build(),
    );
    const dashboardWithoutTitles = dashboard.toBuilder()
      .state(newStates)
      .build();
    const newDashboard = MoveWidgetToTab(widgetId, targetQueryId, dashboardWithoutTitles, false);

    expect(newDashboard).toMatchSnapshot();
  });

  it('should copy a Widget to a dashboard', () => {
    const newDashboard = MoveWidgetToTab(widgetId, targetQueryId, dashboard, true);

    expect(newDashboard).toMatchSnapshot();
  });

  it('should provide a default position in case the widgets position cannot be found', () => {
    const { state } = dashboard;
    const viewState = state.get(sourceQueryId);
    const positions = Immutable.Map(viewState.widgetPositions);
    const newPositions = positions.delete(widgetId);
    const newViewState = viewState.toBuilder().widgetPositions(newPositions.toJS()).build();
    const newState = state.set(sourceQueryId, newViewState);
    const dashboardWithMissingPosition = dashboard.toBuilder().state(newState).build();

    const newDashboard = MoveWidgetToTab(widgetId, targetQueryId, dashboardWithMissingPosition, false);

    expect(newDashboard).toMatchSnapshot();
  });
});
