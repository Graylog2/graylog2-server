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
import { render, screen } from 'wrappedTestingLibrary';

import asMock from 'helpers/mocking/AsMock';
import suppressConsole from 'helpers/suppressConsole';
import GlobalContextProviders from 'contexts/GlobalContextProviders';
import usePluginEntities from 'views/logic/usePluginEntities';

jest.mock('views/logic/usePluginEntities');

const renderProvider = (children, index, throwError = false) => {
  if (throwError) {
    throw Error('The error');
  }

  return (
    <>
      {throwError}
      <div>The provider {index}</div>
      <div>{children}</div>
    </>
  );
};

describe('GlobalContextProviders', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should render children, when there is no global provider', () => {
    asMock(usePluginEntities).mockReturnValue(undefined);

    render(
      <GlobalContextProviders>
        <>The children</>
      </GlobalContextProviders>,
    );

    expect(screen.getByText('The children')).toBeInTheDocument();
  });

  it('should render one plugable global provider', () => {
    asMock(usePluginEntities).mockReturnValue([({ children }) => renderProvider(children, 1)]);

    render(
      <GlobalContextProviders>
        <>The children</>
      </GlobalContextProviders>,
    );

    expect(screen.getByText(/The provider 1/)).toBeInTheDocument();
    expect(screen.getByText('The children')).toBeInTheDocument();
  });

  it('should render multiple plugable global provider', () => {
    asMock(usePluginEntities).mockReturnValue([
      ({ children }) => renderProvider(children, 1),
      ({ children }) => renderProvider(children, 2),
    ]);

    render(
      <GlobalContextProviders>
        <>The children</>
      </GlobalContextProviders>,
    );

    expect(screen.getByText('The provider 1')).toBeInTheDocument();
    expect(screen.getByText('The provider 2')).toBeInTheDocument();
    expect(screen.getByText('The children')).toBeInTheDocument();
  });

  it('should render children and other providers, when one provider throws an error', async () => {
    asMock(usePluginEntities).mockReturnValue([
      ({ children }) => renderProvider(children, 1),
      ({ children }) => renderProvider(children, 2, true),
      ({ children }) => renderProvider(children, 3),
    ]);

    await suppressConsole(() => {
      render(
        <GlobalContextProviders>
          <>The children</>
        </GlobalContextProviders>,
      );
    });

    expect(screen.getByText('The provider 1')).toBeInTheDocument();
    expect(screen.queryByText('The provider 2')).not.toBeInTheDocument();
    expect(screen.getByText('The provider 3')).toBeInTheDocument();
    expect(screen.getByText('The children')).toBeInTheDocument();
  });
});
