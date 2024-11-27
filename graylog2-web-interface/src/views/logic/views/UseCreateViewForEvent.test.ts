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
import { renderHook } from 'wrappedTestingLibrary/hooks';
import ObjectID from 'bson-objectid';

import {
  mockedMappedAggregation,
  mockedMappedAggregationNoField,
  mockedViewWithOneAggregation,
  mockedViewWithOneAggregationNoField,
  mockedViewWithTwoAggregations,
  mockEventData,
  mockEventDefinitionOneAggregation,
  mockEventDefinitionOneAggregationNoFields,
  mockEventDefinitionTwoAggregations,
} from 'helpers/mocking/EventAndEventDefinitions_mock';
import { UseCreateViewForEvent } from 'views/logic/views/UseCreateViewForEvent';
import generateId from 'logic/generateId';
import asMock from 'helpers/mocking/AsMock';
import type View from 'views/logic/views/View';
import { StaticColor } from 'views/logic/views/formatting/highlighting/HighlightingColor';

const counter = () => {
  let index = 0;

  return () => {
    const oldIndex = index;
    index += 1;

    return oldIndex;
  };
};

const generateIdCounterTwoAggregations = counter();
const objectIdCounterTwoAggregations = counter();
const generateIdCounterOneAggregation = counter();
const objectIdCounterOneAggregations = counter();
const generateIdCounterOneAggregationNoField = counter();
const objectIdCounterOneAggregationsNoField = counter();

const mockedGenerateIdTwoAggregations = () => {
  const idSet = ['query-id', 'mc-widget-id', 'allm-widget-id', 'field1-widget-id', 'field2-widget-id', 'summary-widget-id'];
  const index = generateIdCounterTwoAggregations();

  return idSet[index];
};

const mockedObjectIdTwoAggregations = () => {
  const idSet = ['', 'view-id', 'search-id'];
  const index = objectIdCounterTwoAggregations();

  return idSet[index];
};

const mockedObjectIdOneAggregation = () => {
  const idSet = ['', 'view-id', 'search-id'];
  const index = objectIdCounterOneAggregations();

  return idSet[index];
};

const mockedObjectIdOneAggregationNoFields = () => {
  const idSet = ['', 'view-id', 'search-id'];
  const index = objectIdCounterOneAggregationsNoField();

  return idSet[index];
};

const mockedGenerateIdOneAggregation = () => {
  const idSet = ['query-id', 'mc-widget-id', 'allm-widget-id', 'field1-widget-id'];
  const index = generateIdCounterOneAggregation();

  return idSet[index];
};

const mockedGenerateIdOneAggregationNoFields = () => {
  const idSet = ['query-id', 'mc-widget-id', 'allm-widget-id', 'field1-widget-id'];
  const index = generateIdCounterOneAggregationNoField();

  return idSet[index];
};

jest.mock('graylog-web-plugin/plugin', () => ({
  PluginStore: { exports: jest.fn(() => [{ type: 'aggregation', defaults: {} }]) },
}));

jest.mock('logic/generateId', () => jest.fn());

jest.mock('bson-objectid', () => jest.fn());

const mock_color = StaticColor.create('#ffffff');

jest.mock('views/logic/views/formatting/highlighting/HighlightingRule', () => ({
  ...jest.requireActual('views/logic/views/formatting/highlighting/HighlightingRule'),
  randomColor: jest.fn(() => mock_color),
  __esModule: true,
}));

jest.mock('views/logic/Widgets', () => ({
  ...jest.requireActual('views/logic/Widgets'),
  widgetDefinition: () => ({
    searchTypes: () => [{
      type: 'AGGREGATION',
      typeDefinition: {},
    }],
  }),
}));

const todayDate = new Date();
const withCurrentDate = (view: View) => view.toBuilder().createdAt(todayDate).build();

describe('UseCreateViewForEvent', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should create view with 2 aggregation widgets and one summary', async () => {
    asMock(generateId).mockImplementation(mockedGenerateIdTwoAggregations);

    asMock(ObjectID).mockImplementation(() => (({
      toString: () => mockedObjectIdTwoAggregations(),
    }) as ObjectID));

    const { result } = renderHook(() => UseCreateViewForEvent({ eventData: mockEventData.event, eventDefinition: mockEventDefinitionTwoAggregations, aggregations: mockedMappedAggregation }));
    const view = await result.current.then((r) => r);

    expect(withCurrentDate(view)).toEqual(withCurrentDate(mockedViewWithTwoAggregations));
  });

  it('should create view with 1 aggregation widgets and without summary', async () => {
    asMock(generateId).mockImplementation(mockedGenerateIdOneAggregation);

    asMock(ObjectID).mockImplementation(() => (({
      toString: () => mockedObjectIdOneAggregation(),
    }) as ObjectID));

    const { result } = renderHook(() => UseCreateViewForEvent({ eventData: mockEventData.event, eventDefinition: mockEventDefinitionOneAggregation, aggregations: [mockedMappedAggregation[0]] }));
    const view = await result.current.then((r) => r);

    expect(withCurrentDate(view)).toEqual(withCurrentDate(mockedViewWithOneAggregation));
  });

  it('should create view with 1 aggregation widgets when aggregation has no fields and grouping by', async () => {
    asMock(generateId).mockImplementation(mockedGenerateIdOneAggregationNoFields);

    asMock(ObjectID).mockImplementation(() => (({
      toString: () => mockedObjectIdOneAggregationNoFields(),
    }) as ObjectID));

    const { result } = renderHook(() => UseCreateViewForEvent({ eventData: mockEventData.event, eventDefinition: mockEventDefinitionOneAggregationNoFields, aggregations: mockedMappedAggregationNoField }));
    const view = await result.current.then((r) => r);

    expect(withCurrentDate(view)).toEqual(withCurrentDate(mockedViewWithOneAggregationNoField));
  });
});
