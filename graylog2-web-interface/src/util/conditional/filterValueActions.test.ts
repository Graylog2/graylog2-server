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
import filterValueActions, { filterCloudValueActions } from './filterValueActions';

declare let IS_CLOUD: boolean | undefined;

describe('filterValueActions', () => {
  it('should filter items by type', () => {
    const items = [
      { type: 'something', name: 'something' },
      { type: 'delete-me', name: 'delete me' },
    ];

    expect(filterValueActions(items, ['delete-me'])).toEqual([
      { type: 'something', name: 'something' },
    ]);
  });
});

describe('filterCloudValueActions', () => {
  afterEach(() => {
    IS_CLOUD = undefined;
  });

  it('should not filter items by type when not on cloud', () => {
    const items = [
      { type: 'something', name: 'something' },
      { type: 'delete-me', name: 'delete me' },
    ];

    expect(filterCloudValueActions(items, ['delete-me'])).toEqual([
      { type: 'something', name: 'something' },
      { type: 'delete-me', name: 'delete me' },
    ]);
  });

  it('should filter items by type when on cloud', () => {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    IS_CLOUD = true;

    const items = [
      { type: 'something', name: 'something' },
      { type: 'delete-me', name: 'delete me' },
    ];

    expect(filterCloudValueActions(items, ['delete-me'])).toEqual([
      { type: 'something', name: 'something' },
    ]);
  });
});
