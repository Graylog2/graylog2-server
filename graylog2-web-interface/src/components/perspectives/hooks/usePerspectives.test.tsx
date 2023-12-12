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

jest.mock('hooks/usePluginEntities', () => () => ([
  {
    id: 'default',
    title: 'Default Perspective',
    brandComponent: () => {},
    brandLink: '',
  },
  {
    id: 'example-perspective',
    title: 'Example Perspective',
    brandComponent: () => {},
    brandLink: '',
  },
  {
    id: 'unavailable-perspective',
    title: 'Unavailable Perspective',
    brandComponent: () => {},
    brandLink: '',
    useCondition: () => false,
  },
]));

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

    expect(result.current.map(({ id }) => id)).toEqual(['default', 'example-perspective']);
  });

  it('should throw error when being used outside of PerspectivesContext', async () => {
    const { result } = renderHook(() => usePerspectives());

    expect(result.error).toEqual(Error('usePerspectives hook needs to be used inside PerspectivesContext.Provider'));
  });
});
