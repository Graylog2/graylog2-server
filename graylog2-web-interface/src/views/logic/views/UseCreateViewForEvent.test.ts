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
  mockedViewWithMessageAndBarWidget,
  mockEventData,
  mockEventDefinitionTwoAggregations,
} from 'helpers/mocking/EventAndEventDefinitions_mock';
import { UseCreateViewForEvent } from 'views/logic/views/UseCreateViewForEvent';
import generateId from 'logic/generateId';
import asMock from 'helpers/mocking/AsMock';
import type View from 'views/logic/views/View';

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

const mockedGenerateIdTwoAggregations = () => {
  const idSet = ['query-id', 'mc-widget-id', 'allm-widget-id'];
  const index = generateIdCounterTwoAggregations();

  return idSet[index];
};

const mockedObjectIdTwoAggregations = () => {
  const idSet = ['', 'view-id', 'search-id'];
  const index = objectIdCounterTwoAggregations();

  return idSet[index];
};

jest.mock('graylog-web-plugin/plugin', () => ({
  PluginStore: { exports: jest.fn(() => [{ type: 'aggregation', defaults: {} }]) },
}));

jest.mock('logic/generateId', () => jest.fn());

jest.mock('bson-objectid', () => jest.fn());

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

  it('should create view with message and message count widgets', async () => {
    asMock(generateId).mockImplementation(mockedGenerateIdTwoAggregations);

    asMock(ObjectID).mockImplementation(() => ({
      toString: () => mockedObjectIdTwoAggregations(),
    }) as ObjectID);

    const { result } = renderHook(() => UseCreateViewForEvent({ eventData: mockEventData.event, eventDefinition: mockEventDefinitionTwoAggregations }));
    const view = await result.current.then((r) => r);

    expect(withCurrentDate(view)).toEqual(withCurrentDate(mockedViewWithMessageAndBarWidget));
  });
});
