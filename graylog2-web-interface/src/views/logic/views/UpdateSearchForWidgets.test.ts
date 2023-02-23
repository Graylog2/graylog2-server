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

import UpdateSearchForWidgets from './UpdateSearchForWidgets';

import Parameter from '../parameters/Parameter';
import ValueParameter from '../parameters/ValueParameter';

jest.mock('bson-objectid', () => jest.fn(() => ({
  toString: jest.fn(() => 'new-search-id'),
})));

jest.mock('logic/generateId', () => jest.fn(() => 'dead-beef'));

jest.mock('../Widgets', () => ({
  widgetDefinition: () => ({ searchTypes: () => [{ type: 'pivot' }] }),
}));

jest.mock('../SearchType', () => jest.fn(() => ({
  type: 'pivot',
  handler: jest.fn(),
  defaults: {},
})));

const searchView = () => {
  const readFixture = (fixtureName: string) => readJsonFixture(__dirname, fixtureName);
  const viewFixture = View.fromJSON(readFixture('./UpdateSearchForWidgets.View.fixture.json'));
  const searchFixture = Search.fromJSON(readFixture('./UpdateSearchForWidgets.Search.fixture.json'));

  return viewFixture.toBuilder()
    .search(searchFixture)
    .build();
};

describe('UpdateSearchForWidgets', () => {
  beforeEach(() => {
    Parameter.registerSubtype(ValueParameter.type, ValueParameter);
  });

  it('should generate a new search for the view', () => {
    const newView = UpdateSearchForWidgets(searchView());

    expect(newView).toMatchSnapshot();
  });

  it('should keep order of query tabs', () => {
    const newView = UpdateSearchForWidgets(searchView());
    const queryIds = newView.search.queries.map((q) => q.id);

    expect(queryIds).toEqual(Immutable.OrderedSet([
      'f678e6c5-43b1-4200-b5c3-d33eae164dea',
      '770a516d-3bfd-4a91-89c8-c21cb192fdf1',
      '85782551-ec81-42ed-b9f7-020a8de93653',
    ]));
  });
});
