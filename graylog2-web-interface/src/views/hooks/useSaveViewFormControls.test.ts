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
import { renderHook } from '@testing-library/react-hooks';
import DefaultQueryClientProvider from 'DefaultQueryClientProvider';

import { asMock } from 'helpers/mocking';
import usePluginEntities from 'hooks/usePluginEntities';
import useSaveViewFormControls from 'views/hooks/useSaveViewFormControls';

jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));
jest.mock('hooks/usePluginEntities');

describe('useSaveViewFormControls', () => {
  beforeEach(() => {
    asMock(usePluginEntities).mockReturnValue([]);
  });

  it('should return save view form controls from plugin store', async () => {
    const saveViewFromControl = {
      component: () => 'Pluggable component!',
      id: 'example-plugin-component',
    };

    asMock(usePluginEntities).mockImplementation((entityKey) => ({
      'views.components.saveViewForm': [() => saveViewFromControl],
    }[entityKey]));

    const { result } = renderHook(() => useSaveViewFormControls(), { wrapper: DefaultQueryClientProvider });

    expect(result.current).toEqual([saveViewFromControl]);
  });
});
