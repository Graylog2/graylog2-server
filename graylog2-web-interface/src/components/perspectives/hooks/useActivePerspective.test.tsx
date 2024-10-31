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
import React from 'react';
import { renderHook } from 'wrappedTestingLibrary/hooks';

import useActivePerspective from 'components/perspectives/hooks/useActivePerspective';
import PerspectivesProvider from 'components/perspectives/contexts/PerspectivesProvider';
import usePluginEntities from 'hooks/usePluginEntities';
import { asMock } from 'helpers/mocking';
import { defaultPerspective } from 'fixtures/perspectives';

jest.mock('hooks/usePluginEntities');

describe('useActivePerspective', () => {
  beforeEach(() => {
    asMock(usePluginEntities).mockImplementation((entityKey) => ({
      perspectives: [defaultPerspective],
    }[entityKey]));
  });

  const wrapper = ({ children }: React.PropsWithChildren) => (
    <PerspectivesProvider>
      {children}
    </PerspectivesProvider>
  );

  it('should return active perspective', async () => {
    const { result } = renderHook(() => useActivePerspective(), { wrapper });

    expect(result.current.activePerspective).toEqual(defaultPerspective);
  });

  it('should throw error when being used outside of PerspectivesContext', async () => {
    const { result } = renderHook(() => useActivePerspective());

    expect(result.error).toEqual(Error('useActivePerspective hook needs to be used inside PerspectivesContext.Provider'));
  });
});
