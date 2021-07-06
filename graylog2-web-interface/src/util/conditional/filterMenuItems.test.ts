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

import AppConfig from 'util/AppConfig';

import filterMenuItems, { filterCloudMenuItems } from './filterMenuItems';

jest.mock('util/AppConfig', () => ({
  isCloud: jest.fn(() => false),
}));

describe('filterMenuItems', () => {
  it('should filter items by path', () => {
    const items = [
      { path: 'something', name: 'something' },
      { path: 'delete-me', name: 'delete me' },
    ];

    expect(filterMenuItems(items, ['delete-me'])).toEqual([
      { path: 'something', name: 'something' },
    ]);
  });
});

describe('filterCloudMenuItem', () => {
  afterEach(() => {
    jest.resetAllMocks();
  });

  it('should not filter items by path when not on cloud', () => {
    const items = [
      { path: 'something', name: 'something' },
      { path: 'delete-me', name: 'delete me' },
    ];

    expect(filterCloudMenuItems(items, ['delete-me'])).toEqual([
      { path: 'something', name: 'something' },
      { path: 'delete-me', name: 'delete me' },
    ]);
  });

  it('should filter items by path when on cloud', () => {
    asMock(AppConfig.isCloud).mockReturnValue(true);

    const items = [
      { path: 'something', name: 'something' },
      { path: 'delete-me', name: 'delete me' },
    ];

    expect(filterCloudMenuItems(items, ['delete-me'])).toEqual([
      { path: 'something', name: 'something' },
    ]);
  });
});
