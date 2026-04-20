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
import { defaultUser } from 'defaultMockValues';
import Immutable from 'immutable';

import asMock from 'helpers/mocking/AsMock';
import useCurrentUser from 'hooks/useCurrentUser';
import type { Attribute } from 'stores/PaginationTypes';

import useAuthorizedAttributes from './useAuthorizedAttributes';

jest.mock('hooks/useCurrentUser');

declare module 'graylog-web-plugin/plugin' {
  interface EntityActions {
    restricted: 'read';
  }
}

describe('useAuthorizedAttributes', () => {
  const attributes: Array<Attribute> = [
    { id: 'title', title: 'Title', type: 'STRING' },
    { id: 'description', title: 'Description', type: 'STRING' },
    { id: 'restricted', title: 'Restricted', type: 'STRING', permissions: ['restricted:read'] },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useCurrentUser).mockReturnValue(defaultUser);
  });

  it('should exclude attributes the user lacks permissions for', () => {
    asMock(useCurrentUser).mockReturnValue(
      defaultUser.toBuilder().permissions(Immutable.List()).build(),
    );

    const { result } = renderHook(() => useAuthorizedAttributes(attributes));

    const ids = result.current.map(({ id }) => id);

    expect(ids).toContain('title');
    expect(ids).toContain('description');
    expect(ids).not.toContain('restricted');
  });

  it('should include attributes the user has permissions for', () => {
    asMock(useCurrentUser).mockReturnValue(
      defaultUser.toBuilder().permissions(Immutable.List(['restricted:read'])).build(),
    );

    const { result } = renderHook(() => useAuthorizedAttributes(attributes));

    const ids = result.current.map(({ id }) => id);

    expect(ids).toContain('title');
    expect(ids).toContain('description');
    expect(ids).toContain('restricted');
  });

  it('should include attributes without permissions defined', () => {
    asMock(useCurrentUser).mockReturnValue(
      defaultUser.toBuilder().permissions(Immutable.List()).build(),
    );

    const { result } = renderHook(() => useAuthorizedAttributes(attributes));

    expect(result.current).toHaveLength(2);
  });
});
