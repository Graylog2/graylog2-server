import { readFileSync } from 'fs';
import { dirname } from 'path';

import Search from 'views/logic/search/Search';
import View from 'views/logic/views/View';
import copyWidgetToDashboard from './CopyWidgetToDashboard';

const cwd = dirname(__filename);
const readFixture = filename => JSON.parse(readFileSync(`${cwd}/${filename}`).toString());

describe('copyWidgetToDashboard', () => {
  it('should copy a Widget to a dashboard', () => {
    const searchViewFixture = View.fromJSON(readFixture('./CopyWidgetToDashboard.Search-View.fixture.json'));
    const searchSearchFixture = Search.fromJSON(readFixture('./CopyWidgetToDashboard.Search-Search.fixture.json'));
    const searchView = searchViewFixture.toBuilder()
      .search(searchSearchFixture)
      .build();

    const dashboardViewFixture = View.fromJSON(readFixture('./CopyWidgetToDashboard.Dashboard-View.fixture.json'));
    const dashboardSearchFixture = Search.fromJSON(readFixture('./CopyWidgetToDashboard.Dashboard-Search.fixture.json'));
    const dashboardView = dashboardViewFixture.toBuilder()
      .search(dashboardSearchFixture)
      .build();

    const widgetId = '4d73ccaa-aabf-451a-b36e-309f55798e04';

    const newDashboard = copyWidgetToDashboard(widgetId, searchView, dashboardView);
    expect(newDashboard).toMatchSnapshot();
  });
});
