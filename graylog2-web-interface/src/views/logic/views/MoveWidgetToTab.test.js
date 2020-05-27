import { readFileSync } from 'fs';
import { dirname } from 'path';

import MoveWidgetToTab from './MoveWidgetToTab';
import Parameter from '../parameters/Parameter';
import ValueParameter from '../parameters/ValueParameter';
import View from './View';
import Search from '../search/Search';

jest.mock('uuid/v4', () => jest.fn(() => 'dead-beef'));

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

describe('MoveWidgetToTab', () => {
  beforeEach(() => {
    Parameter.registerSubtype(ValueParameter.type, ValueParameter);
  });

  it('should move a Widget to a dashboard', () => {
    const dashboardFixture = View.fromJSON(readFixture('./MoveWidgetToTab.Dashboard.fixture.json'));
    const searchFixture = Search.fromJSON(readFixture('./MoveWidgetToTab.Search.fixture.json'));
    const dashboaad = dashboardFixture.toBuilder()
      .search(searchFixture)
      .build();

    const newDashboard = MoveWidgetToTab(
      'b34c3c6f-c49d-41d3-a65a-f746134f8f3e',
      '5faea09b-4187-4eda-9d59-7a86d4774c73',
      dashboaad,
      false,
    );

    expect(newDashboard).toMatchSnapshot();
  });

  it('should copy a Widget to a dashboard', () => {
    const dashboardFixture = View.fromJSON(readFixture('./MoveWidgetToTab.Dashboard.fixture.json'));
    const searchFixture = Search.fromJSON(readFixture('./MoveWidgetToTab.Search.fixture.json'));
    const dashboaad = dashboardFixture.toBuilder()
      .search(searchFixture)
      .build();

    const newDashboard = MoveWidgetToTab(
      'b34c3c6f-c49d-41d3-a65a-f746134f8f3e',
      '5faea09b-4187-4eda-9d59-7a86d4774c73',
      dashboaad,
      true,
    );

    expect(newDashboard).toMatchSnapshot();
  });
});
