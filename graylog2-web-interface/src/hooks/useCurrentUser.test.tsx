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
import * as React from 'react';
import { renderHook } from '@testing-library/react-hooks';

import { adminUser } from 'fixtures/users';
import CurrentUserContext from 'contexts/CurrentUserContext';
import useCurrentUser from 'hooks/useCurrentUser';

describe('useCurrentUser', () => {
  it('should return value of CurrentUserContext', () => {
    const Wrapper = ({ children }: {children: React.ReactNode}) => (
      <CurrentUserContext.Provider value={adminUser}>
        {children}
      </CurrentUserContext.Provider>
    );

    const { result } = renderHook(() => useCurrentUser(), { wrapper: Wrapper });

    expect(result.current).toBe(adminUser);
  });

  it('should throw error when being used outside of CurrentUserContext.Provider', () => {
    const result = renderHook(() => useCurrentUser());

    expect(result.result.error).toEqual(new Error('useCurrentUser hook needs to be used inside CurrentUserContext.Provider'));
  });
});
