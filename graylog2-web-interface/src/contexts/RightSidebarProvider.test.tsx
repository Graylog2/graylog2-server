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
  const { isOpen, content, width, openSidebar, closeSidebar, setWidth, updateContent, goBack, goForward, canGoBack, canGoForward } = useRightSidebar();

  return (
    <div>
      <div data-testid="is-open">{String(isOpen)}</div>
      <div data-testid="content-id">{content?.id || 'null'}</div>
      <div data-testid="width">{width}</div>
      <div data-testid="can-go-back">{String(canGoBack)}</div>
      <div data-testid="can-go-forward">{String(canGoForward)}</div>
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
      <button
        type="button"
        onClick={() =>
          openSidebar({
            id: 'test-sidebar-2',
            title: 'Test Sidebar 2',
            component: TestComponent,
          })
        }>
        Open Sidebar 2
      </button>
      <button
        type="button"
        onClick={() =>
          openSidebar({
            id: 'test-sidebar-3',
            title: 'Test Sidebar 3',
            component: TestComponent,
          })
        }>
        Open Sidebar 3
      </button>
      <button
        type="button"
        onClick={() =>
          updateContent({
            id: 'updated-sidebar',
            title: 'Updated Sidebar',
            component: TestComponent,
          })
        }>
        Update Content
      </button>
      <button type="button" onClick={closeSidebar}>
        Close Sidebar
      </button>
      <button type="button" onClick={() => setWidth(500)}>
        Set Width 500
      </button>
      <button type="button" onClick={goBack}>
        Go Back
      </button>
      <button type="button" onClick={goForward}>
        Go Forward
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

    expect(screen.getByTestId('is-open')).toHaveTextContent('false');
    expect(screen.getByTestId('content-id')).toHaveTextContent('null');
    expect(screen.getByTestId('width')).toHaveTextContent('400');
    expect(screen.getByTestId('can-go-back')).toHaveTextContent('false');
    expect(screen.getByTestId('can-go-forward')).toHaveTextContent('false');
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

  describe('Navigation History', () => {
    it('should add content to history when opening sidebar', async () => {
      render(
        <RightSidebarProvider>
          <TestConsumer />
        </RightSidebarProvider>,
      );

      await userEvent.click(screen.getByText('Open Sidebar'));
      expect(screen.getByTestId('content-id')).toHaveTextContent('test-sidebar');
      expect(screen.getByTestId('can-go-back')).toHaveTextContent('false');

      await userEvent.click(screen.getByText('Open Sidebar 2'));
      expect(screen.getByTestId('content-id')).toHaveTextContent('test-sidebar-2');
      expect(screen.getByTestId('can-go-back')).toHaveTextContent('true');
    });

    it('should navigate back to previous content', async () => {
      render(
        <RightSidebarProvider>
          <TestConsumer />
        </RightSidebarProvider>,
      );

      await userEvent.click(screen.getByText('Open Sidebar'));
      await userEvent.click(screen.getByText('Open Sidebar 2'));

      expect(screen.getByTestId('content-id')).toHaveTextContent('test-sidebar-2');

      await userEvent.click(screen.getByText('Go Back'));

      expect(screen.getByTestId('content-id')).toHaveTextContent('test-sidebar');
      expect(screen.getByTestId('can-go-back')).toHaveTextContent('false');
      expect(screen.getByTestId('can-go-forward')).toHaveTextContent('true');
    });

    it('should navigate forward after going back', async () => {
      render(
        <RightSidebarProvider>
          <TestConsumer />
        </RightSidebarProvider>,
      );

      await userEvent.click(screen.getByText('Open Sidebar'));
      await userEvent.click(screen.getByText('Open Sidebar 2'));
      await userEvent.click(screen.getByText('Go Back'));

      expect(screen.getByTestId('content-id')).toHaveTextContent('test-sidebar');

      await userEvent.click(screen.getByText('Go Forward'));

      expect(screen.getByTestId('content-id')).toHaveTextContent('test-sidebar-2');
      expect(screen.getByTestId('can-go-forward')).toHaveTextContent('false');
    });

    it('should disable back button at beginning of history', async () => {
      render(
        <RightSidebarProvider>
          <TestConsumer />
        </RightSidebarProvider>,
      );

      await userEvent.click(screen.getByText('Open Sidebar'));

      expect(screen.getByTestId('can-go-back')).toHaveTextContent('false');
    });

    it('should disable forward button at end of history', async () => {
      render(
        <RightSidebarProvider>
          <TestConsumer />
        </RightSidebarProvider>,
      );

      await userEvent.click(screen.getByText('Open Sidebar'));
      await userEvent.click(screen.getByText('Open Sidebar 2'));

      expect(screen.getByTestId('can-go-forward')).toHaveTextContent('false');
    });

    it('should truncate forward history when opening new content mid-stack', async () => {
      render(
        <RightSidebarProvider>
          <TestConsumer />
        </RightSidebarProvider>,
      );

      await userEvent.click(screen.getByText('Open Sidebar'));
      await userEvent.click(screen.getByText('Open Sidebar 2'));
      await userEvent.click(screen.getByText('Open Sidebar 3'));

      await userEvent.click(screen.getByText('Go Back'));
      await userEvent.click(screen.getByText('Go Back'));

      expect(screen.getByTestId('content-id')).toHaveTextContent('test-sidebar');
      expect(screen.getByTestId('can-go-forward')).toHaveTextContent('true');

      await userEvent.click(screen.getByText('Open Sidebar 2'));

      expect(screen.getByTestId('content-id')).toHaveTextContent('test-sidebar-2');
      expect(screen.getByTestId('can-go-forward')).toHaveTextContent('false');
    });

    it('should clear history when closing sidebar', async () => {
      render(
        <RightSidebarProvider>
          <TestConsumer />
        </RightSidebarProvider>,
      );

      await userEvent.click(screen.getByText('Open Sidebar'));
      await userEvent.click(screen.getByText('Open Sidebar 2'));

      expect(screen.getByTestId('content-id')).toHaveTextContent('test-sidebar-2');
      expect(screen.getByTestId('can-go-back')).toHaveTextContent('true');

      await userEvent.click(screen.getByText('Close Sidebar'));
      expect(screen.getByTestId('is-open')).toHaveTextContent('false');
      expect(screen.getByTestId('content-id')).toHaveTextContent('null');
      expect(screen.getByTestId('can-go-back')).toHaveTextContent('false');
      expect(screen.getByTestId('can-go-forward')).toHaveTextContent('false');

      await userEvent.click(screen.getByText('Open Sidebar'));

      expect(screen.getByTestId('is-open')).toHaveTextContent('true');
      expect(screen.getByTestId('content-id')).toHaveTextContent('test-sidebar');
      expect(screen.getByTestId('can-go-back')).toHaveTextContent('false');
    });

    it('should update content without affecting history', async () => {
      render(
        <RightSidebarProvider>
          <TestConsumer />
        </RightSidebarProvider>,
      );

      await userEvent.click(screen.getByText('Open Sidebar'));
      await userEvent.click(screen.getByText('Open Sidebar 2'));

      expect(screen.getByTestId('content-id')).toHaveTextContent('test-sidebar-2');
      expect(screen.getByTestId('can-go-back')).toHaveTextContent('true');

      await userEvent.click(screen.getByText('Update Content'));

      expect(screen.getByTestId('content-id')).toHaveTextContent('updated-sidebar');
      expect(screen.getByTestId('can-go-back')).toHaveTextContent('true');

      await userEvent.click(screen.getByText('Go Back'));

      expect(screen.getByTestId('content-id')).toHaveTextContent('test-sidebar');
    });

    it('should handle navigation at boundaries gracefully', async () => {
      render(
        <RightSidebarProvider>
          <TestConsumer />
        </RightSidebarProvider>,
      );

      await userEvent.click(screen.getByText('Open Sidebar'));

      await userEvent.click(screen.getByText('Go Back'));
      expect(screen.getByTestId('content-id')).toHaveTextContent('test-sidebar');

      await userEvent.click(screen.getByText('Go Forward'));
      expect(screen.getByTestId('content-id')).toHaveTextContent('test-sidebar');
    });
  });
});
