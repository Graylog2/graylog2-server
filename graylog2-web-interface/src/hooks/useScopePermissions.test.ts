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
import { waitFor } from 'wrappedTestingLibrary';

import { asMock } from 'helpers/mocking';
import fetch from 'logic/rest/FetchProvider';

import useScopePermissions from './useScopePermissions';

jest.mock('logic/rest/FetchProvider', () => jest.fn());

const entityScopes = {
  entity_scopes: {
    DEFAULT: { is_mutable: true, is_deletable: true },
    ILLUMINATE: { is_mutable: false, is_deletable: true },
  },
};

describe('useScopePermissions', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('returns the permissions of the entity scope', async () => {
    asMock(fetch).mockResolvedValue(entityScopes);

    const { result } = renderHook(() => useScopePermissions({ _scope: 'illuminate' }));

    await waitFor(() => expect(result.current.loadingScopePermissions).toBe(false));

    expect(result.current.scopePermissions).toEqual({ is_mutable: false, is_deletable: true });
    expect(result.current.checkPermissions({ _scope: 'DEFAULT' })).toBe(true);
  });

  it('uses the default scope for entities without a scope', async () => {
    asMock(fetch).mockResolvedValue(entityScopes);

    const { result } = renderHook(() => useScopePermissions({}));

    await waitFor(() => expect(result.current.loadingScopePermissions).toBe(false));

    expect(result.current.scopePermissions.is_mutable).toBe(true);
  });

  it('treats unknown scopes as not mutable', async () => {
    asMock(fetch).mockResolvedValue(entityScopes);

    const { result } = renderHook(() => useScopePermissions({ _scope: 'UNKNOWN' }));

    await waitFor(() => expect(result.current.loadingScopePermissions).toBe(false));

    expect(result.current.scopePermissions).toEqual({ is_mutable: false, is_deletable: false });
    expect(result.current.checkPermissions({ _scope: 'UNKNOWN' })).toBe(false);
  });

  it('treats a response without scopes as not mutable', async () => {
    asMock(fetch).mockResolvedValue({});

    const { result } = renderHook(() => useScopePermissions({ _scope: 'DEFAULT' }));

    await waitFor(() => expect(result.current.loadingScopePermissions).toBe(false));

    expect(result.current.scopePermissions.is_mutable).toBe(false);
    expect(result.current.checkPermissions({ _scope: 'DEFAULT' })).toBe(false);
  });
});
