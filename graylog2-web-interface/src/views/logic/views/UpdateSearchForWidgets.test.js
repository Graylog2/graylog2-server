import { readFileSync } from 'fs';
import { dirname } from 'path';

import Search from 'views/logic/search/Search';
import View from 'views/logic/views/View';

import UpdateSearchForWidgets from './UpdateSearchForWidgets';
import Parameter from '../parameters/Parameter';
import ValueParameter from '../parameters/ValueParameter';

const cwd = dirname(__filename);
const readFixture = (filename) => JSON.parse(readFileSync(`${cwd}/${filename}`).toString());

jest.mock('bson-objectid', () => jest.fn(() => ({
  toString: jest.fn(() => 'new-search-id'),
})));
jest.mock('uuid/v4', () => jest.fn(() => 'dead-beef'));

jest.mock('../Widgets', () => ({
  widgetDefinition: () => ({ searchTypes: () => [{ type: 'pivot' }] }),
}));

jest.mock('../SearchType', () => jest.fn(() => ({
  type: 'pivot',
  handler: jest.fn(),
  defaults: {},
})));

describe('UpdateSearchForWidgets', () => {
  beforeEach(() => {
    Parameter.registerSubtype(ValueParameter.type, ValueParameter);
  });

  it('should generate a new search for the view', () => {
    const viewFixture = View.fromJSON(readFixture('./UpdateSearchForWidgets.View.fixture.json'));
    const searchFixture = Search.fromJSON(readFixture('./UpdateSearchForWidgets.Search.fixture.json'));
    const searchView = viewFixture.toBuilder()
      .search(searchFixture)
      .build();

    const newView = UpdateSearchForWidgets(searchView);
    expect(newView).toMatchSnapshot();
  });
});
