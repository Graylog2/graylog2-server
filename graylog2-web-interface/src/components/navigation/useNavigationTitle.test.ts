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
import { renderHook, act } from 'wrappedTestingLibrary/hooks';

import { changeLanguage } from 'i18n';

import useNavigationTitle from './useNavigationTitle';

describe('useNavigationTitle', () => {
  afterEach(() => changeLanguage('en'));

  it('uses the description of items without title key', () => {
    const { result } = renderHook(() => useNavigationTitle());

    expect(result.current({ description: 'Plugin Page' })).toBe('Plugin Page');
  });

  it('translates items with title key', async () => {
    const { result } = renderHook(() => useNavigationTitle());
    const item = { description: 'Search', titleKey: 'navigation:main.search' };

    expect(result.current(item)).toBe('Search');

    await act(() => changeLanguage('de'));

    expect(result.current(item)).toBe('Suche');
  });
});
