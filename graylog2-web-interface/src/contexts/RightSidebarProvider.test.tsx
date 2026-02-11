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
import userEvent from '@testing-library/user-event';

import RightSidebarProvider from './RightSidebarProvider';
import useRightSidebar from '../hooks/useRightSidebar';

const TestComponent = () => <div>Test Content</div>;

const TestConsumer = () => {
  const { isOpen, content, width, openSidebar, closeSidebar, setWidth } = useRightSidebar();

  return (
    <div>
      <div data-testid="is-open">{String(isOpen)}</div>
      <div data-testid="content-id">{content?.id || 'null'}</div>
      <div data-testid="width">{width}</div>
      <button
        type="button"
        onClick={() =>
          openSidebar({
            id: 'test-sidebar',
            title: 'Test Sidebar',
            component: TestComponent,
            props: { testProp: 'value' },
          })
        }>
        Open Sidebar
      </button>
      <button type="button" onClick={closeSidebar}>
        Close Sidebar
      </button>
      <button type="button" onClick={() => setWidth(500)}>
        Set Width 500
      </button>
    </div>
  );
};

describe('RightSidebarProvider', () => {
  it('should provide default context values', () => {
    render(
      <RightSidebarProvider>
        <TestConsumer />
      </RightSidebarProvider>,
    );

    expect(screen.getByTestId('is-open')).toHaveTextContent('true');
    expect(screen.getByTestId('content-id')).toHaveTextContent('null');
    expect(screen.getByTestId('width')).toHaveTextContent('400');
  });

  it('should open sidebar with content', async () => {
    render(
      <RightSidebarProvider>
        <TestConsumer />
      </RightSidebarProvider>,
    );

    await userEvent.click(screen.getByText('Open Sidebar'));

    expect(screen.getByTestId('is-open')).toHaveTextContent('true');
    expect(screen.getByTestId('content-id')).toHaveTextContent('test-sidebar');
  });

  it('should close sidebar', async () => {
    render(
      <RightSidebarProvider>
        <TestConsumer />
      </RightSidebarProvider>,
    );

    await userEvent.click(screen.getByText('Open Sidebar'));
    expect(screen.getByTestId('is-open')).toHaveTextContent('true');

    await userEvent.click(screen.getByText('Close Sidebar'));
    expect(screen.getByTestId('is-open')).toHaveTextContent('false');
  });

  it('should update sidebar width', async () => {
    render(
      <RightSidebarProvider>
        <TestConsumer />
      </RightSidebarProvider>,
    );

    expect(screen.getByTestId('width')).toHaveTextContent('400');

    await userEvent.click(screen.getByText('Set Width 500'));

    expect(screen.getByTestId('width')).toHaveTextContent('500');
  });
});
