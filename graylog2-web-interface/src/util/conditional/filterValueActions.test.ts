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
import asMock from 'helpers/mocking/AsMock';

import { ActionDefinition } from 'views/components/actions/ActionHandler';
import AppConfig from 'util/AppConfig';

import filterValueActions, { filterCloudValueActions } from './filterValueActions';

jest.mock('util/AppConfig', () => ({
  isCloud: jest.fn(() => false),
}));

const items: Array<ActionDefinition> = [
  { type: 'something', title: 'something', resetFocus: false },
  { type: 'delete-me', title: 'delete me', resetFocus: false },
];

describe('filterValueActions', () => {
  it('should filter items by type', () => {
    expect(filterValueActions(items, ['delete-me'])).toEqual(items.filter((item) => item.type !== 'delete-me'));
  });
});

describe('filterCloudValueActions', () => {
  afterEach(() => {
    jest.resetAllMocks();
  });

  it('should not filter items by type when not on cloud', () => {
    expect(filterCloudValueActions(items, ['delete-me'])).toEqual(items);
  });

  it('should filter items by type when on cloud', () => {
    asMock(AppConfig.isCloud).mockReturnValue(true);

    expect(filterCloudValueActions(items, ['delete-me'])).toEqual(items.filter((item) => item.type !== 'delete-me'));
  });
});
