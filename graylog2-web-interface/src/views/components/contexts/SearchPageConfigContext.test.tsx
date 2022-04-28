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
import { render } from 'wrappedTestingLibrary';

import type { SearchConfigState } from 'views/components/contexts/SearchPageConfigContext';

import { SearchPageConfigContext, SearchPageConfigContextProvider, ViewActionsLayoutOptions } from './SearchPageConfigContext';

describe('SearchPageConfigProvider', () => {
  const SUT = (suppliedProviderOverrides : SearchConfigState = undefined) => {
    let contextValue;

    render(
      <SearchPageConfigContextProvider providerOverrides={suppliedProviderOverrides}>
        <SearchPageConfigContext.Consumer>
          {(value) => {
            contextValue = value;

            return <div />;
          }}
        </SearchPageConfigContext.Consumer>

      </SearchPageConfigContextProvider>,
    );

    return contextValue;
  };

  it('provides logical defaults when no provider overrides are supplied', () => {
    const providerOverrides: SearchConfigState = {
      sidebar: { isShown: true }, viewActionsLayoutOptions: ViewActionsLayoutOptions.FULL_MENU,
    };
    const contextValue = SUT();

    expect(contextValue).toEqual(providerOverrides);
  });

  it('provides the overridden provider state when supplied', () => {
    const providerOverrides: SearchConfigState = {
      sidebar: { isShown: false }, viewActionsLayoutOptions: ViewActionsLayoutOptions.SAVE_COPY,
    };
    const contextValue = SUT(providerOverrides);

    expect(contextValue).toEqual(providerOverrides);
  });
});
