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
import * as React from 'react';
import { render } from '@testing-library/react';

import HighlightMessageInQuery from './HighlightMessageInQuery';

import HighlightMessageContext from '../contexts/HighlightMessageContext';

// eslint-disable-next-line react/prop-types
jest.mock('routing/withLocation', () => (Component) => ({ query, ...rest }) => <Component location={{ query }} {...rest} />);

describe('HighlightMessageInQuery', () => {
  const TestComponent = () => (
    <HighlightMessageContext.Consumer>
      {(messageId) => <span>{messageId}</span>}
    </HighlightMessageContext.Consumer>
  );

  it('should render component for empty query', () => {
    const { container } = render((
      <HighlightMessageInQuery query={undefined}>
        <TestComponent />
      </HighlightMessageInQuery>
    ));

    expect(container).not.toBeNull();
  });

  it('should render component for query without message id', () => {
    const { container } = render((
      <HighlightMessageInQuery query={{}}>
        <TestComponent />
      </HighlightMessageInQuery>
    ));

    expect(container).not.toBeNull();
  });

  it('should pass message id from query to children', () => {
    const { getByText } = render((
      <HighlightMessageInQuery query={{ highlightMessage: 'foobar' }}>
        <TestComponent />
      </HighlightMessageInQuery>
    ));

    expect(getByText('foobar')).not.toBeNull();
  });
});
