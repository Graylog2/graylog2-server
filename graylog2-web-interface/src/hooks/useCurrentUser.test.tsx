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
