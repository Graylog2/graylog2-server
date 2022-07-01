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

import type { LayoutState } from 'views/components/contexts/SearchPageLayoutContext';
import SearchPageLayoutContext, { SAVE_COPY } from 'views/components/contexts/SearchPageLayoutContext';

describe('SearchPageConfigProvider', () => {
  const SUT = (suppliedProviderOverrides : LayoutState = undefined) => {
    let contextValue;

    render(
      <SearchPageLayoutContext.Provider value={suppliedProviderOverrides}>
        <SearchPageLayoutContext.Consumer>
          {(value) => {
            contextValue = value;

            return <div />;
          }}
        </SearchPageLayoutContext.Consumer>

      </SearchPageLayoutContext.Provider>,
    );

    return contextValue;
  };

  it('provides the overridden provider state when supplied', () => {
    const providerOverrides: LayoutState = {
      sidebar: { isShown: false }, viewActions: SAVE_COPY,
    };
    const contextValue = SUT(providerOverrides);

    expect(contextValue).toEqual(providerOverrides);
  });
});
