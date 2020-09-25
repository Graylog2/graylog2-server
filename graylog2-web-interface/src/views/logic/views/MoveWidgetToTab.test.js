// @flow strict
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

const cwd = dirname(__filename);
const readFixture = (filename) => JSON.parse(readFileSync(`${cwd}/${filename}`).toString());

const dashboardFixture = View.fromJSON(readFixture('./MoveWidgetToTab.Dashboard.fixture.json'));
const searchFixture = Search.fromJSON(readFixture('./MoveWidgetToTab.Search.fixture.json'));
const dashboard = dashboardFixture.toBuilder()
  .search(searchFixture)
  .build();

const widgetId = 'b34c3c6f-c49d-41d3-a65a-f746134f8f3e';
const queryId = '5faea09b-4187-4eda-9d59-7a86d4774c73';

describe('MoveWidgetToTab', () => {
  beforeEach(() => {
    Parameter.registerSubtype(ValueParameter.type, ValueParameter);
  });

  it('should move a Widget to a dashboard', () => {
    const newDashboard = MoveWidgetToTab(widgetId, queryId, dashboard, false);

    expect(newDashboard).toMatchSnapshot();
  });

  it('should work when titles are empty', () => {
    const sourceQueryId = 'fa247d8f-7afa-45b7-b57e-3cdef4ee53be';
    const newStates = dashboard.state.update(
      sourceQueryId,
      (query) => query.toBuilder().titles(Immutable.Map()).build(),
    );
    const dashboardWithoutTitles = dashboard.toBuilder()
      .state(newStates)
      .build();
    const newDashboard = MoveWidgetToTab(widgetId, queryId, dashboardWithoutTitles, false);

    expect(newDashboard).toMatchSnapshot();
  });

  it('should copy a Widget to a dashboard', () => {
    const newDashboard = MoveWidgetToTab(widgetId, queryId, dashboard, true);

    expect(newDashboard).toMatchSnapshot();
  });
});
