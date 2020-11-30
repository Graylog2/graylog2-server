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
// @flow strict
import React from 'react';
import { render } from 'wrappedTestingLibrary';
import suppressConsole from 'helpers/suppressConsole';

import RouterErrorBoundary from './RouterErrorBoundary';

const ErroneusComponent = () => {
  // eslint-disable-next-line no-throw-literal
  throw {
    message: 'Oh no, a banana peel fell on the party gorilla\'s head!',
    stack: 'This the stack trace.',
  };
};

const WorkingComponent = () => <div>Hello World!</div>;

describe('RouterErrorBoundary', () => {
  it('displays child component if there is no error', () => {
    const { getByText } = render(
      <RouterErrorBoundary>
        <WorkingComponent />
      </RouterErrorBoundary>,
    );

    expect(getByText('Hello World!')).not.toBeNull();
  });

  it('displays error after catching', () => {
    suppressConsole(() => {
      const { getByText } = render(
        <RouterErrorBoundary>
          <ErroneusComponent />
        </RouterErrorBoundary>,
      );

      expect(getByText('Oh no, a banana peel fell on the party gorilla\'s head!')).not.toBeNull();
    });
  });
});
