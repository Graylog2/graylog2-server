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

import usePerspectives from 'components/perspectives/hooks/usePerspectives';
import PerspectivesProvider from 'components/perspectives/contexts/PerspectivesProvider';
import { defaultPerspective, examplePerspective, unavailablePerspective } from 'fixtures/perspectives';

const mockedPerspectives = [defaultPerspective, examplePerspective, unavailablePerspective];
jest.mock('hooks/usePluginEntities', () => () => mockedPerspectives);

describe('usePerspectives', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  const wrapper = ({ children }: React.PropsWithChildren) => (
    <PerspectivesProvider>
      {children}
    </PerspectivesProvider>
  );

  it('should return available perspectives', async () => {
    const { result } = renderHook(() => usePerspectives(), { wrapper });

    expect(result.current.map(({ id }) => id)).toEqual([defaultPerspective.id, examplePerspective.id]);
  });

  it('should throw error when being used outside of PerspectivesContext', async () => {
    const { result } = renderHook(() => usePerspectives());

    expect(result.error).toEqual(Error('usePerspectives hook needs to be used inside PerspectivesContext.Provider'));
  });
});
