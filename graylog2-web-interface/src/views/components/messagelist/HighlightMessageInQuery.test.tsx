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
import { render, screen } from '@testing-library/react';

import { asMock } from 'helpers/mocking';
import useQuery from 'routing/useQuery';

import _HighlightMessageInQuery from './HighlightMessageInQuery';

import HighlightMessageContext from '../contexts/HighlightMessageContext';

jest.mock('routing/useQuery');

const HighlightMessageInQuery = _HighlightMessageInQuery as React.ComponentType<React.ComponentProps<typeof _HighlightMessageInQuery> & { query?: any }>;

describe('HighlightMessageInQuery', () => {
  const TestComponent = () => (
    <HighlightMessageInQuery>
      <HighlightMessageContext.Consumer>
        {(messageId) => (messageId ? <span>Message Id: {messageId}</span> : <span>No message is highlighted</span>)}
      </HighlightMessageContext.Consumer>
    </HighlightMessageInQuery>
  );

  it('should render component for empty query', async () => {
    asMock(useQuery).mockReturnValue(undefined);
    render(<TestComponent />);

    await screen.findByText('No message is highlighted');
  });

  it('should render component for query without message id', async () => {
    asMock(useQuery).mockReturnValue({});
    render(<TestComponent />);

    await screen.findByText('No message is highlighted');
  });

  it('should pass message id from query to children', async () => {
    asMock(useQuery).mockReturnValue({ highlightMessage: 'foobar' });
    render(<TestComponent />);

    await screen.findByText('Message Id: foobar');
  });
});
