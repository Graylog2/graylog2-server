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

import RightSidebar from './RightSidebar';

import RightSidebarProvider from '../../../contexts/RightSidebarProvider';
import RightSidebarContext from '../../../contexts/RightSidebarContext';
import useRightSidebar from '../../../hooks/useRightSidebar';

const TestSidebarContent = ({ message = 'Default message' }: { message?: string }) => (
  <div>
    <p>{message}</p>
    <p>Test sidebar content</p>
  </div>
);

const TestWrapper = ({ children }: { children: React.ReactNode }) => (
  <RightSidebarProvider>{children}</RightSidebarProvider>
);

// Match the actual App.tsx usage with Context.Consumer
const SidebarWithConsumer = () => (
  <RightSidebarContext.Consumer>{({ isOpen }) => isOpen && <RightSidebar />}</RightSidebarContext.Consumer>
);

const TriggerButton = () => {
  const { openSidebar } = useRightSidebar();

  return (
    <button
      type="button"
      onClick={() =>
        openSidebar({
          id: 'test-sidebar',
          title: 'Test Sidebar Title',
          component: TestSidebarContent,
          props: { message: 'Hello from sidebar' },
        })
      }>
      Open Sidebar
    </button>
  );
};

describe('RightSidebar', () => {
  it('should not render when content is null', () => {
    render(
      <TestWrapper>
        <SidebarWithConsumer />
      </TestWrapper>,
    );

    // Sidebar should not be present when content is null
    expect(screen.queryByRole('complementary')).not.toBeInTheDocument();
  });

  it('should render sidebar when opened', async () => {
    render(
      <TestWrapper>
        <TriggerButton />
        <SidebarWithConsumer />
      </TestWrapper>,
    );

    const openButton = screen.getByRole('button', { name: /open sidebar/i });
    await userEvent.click(openButton);

    expect(screen.getByText('Test Sidebar Title')).toBeInTheDocument();
    expect(screen.getByText('Hello from sidebar')).toBeInTheDocument();
    expect(screen.getByText('Test sidebar content')).toBeInTheDocument();
  });

  it('should render close button', async () => {
    render(
      <TestWrapper>
        <TriggerButton />
        <SidebarWithConsumer />
      </TestWrapper>,
    );

    const openButton = screen.getByRole('button', { name: /open sidebar/i });
    await userEvent.click(openButton);

    const closeButton = screen.getByRole('button', { name: /close sidebar/i });

    expect(closeButton).toBeInTheDocument();
  });

  it('should close sidebar when close button is clicked', async () => {
    render(
      <TestWrapper>
        <TriggerButton />
        <SidebarWithConsumer />
      </TestWrapper>,
    );

    // Open sidebar
    const openButton = screen.getByRole('button', { name: /open sidebar/i });
    await userEvent.click(openButton);

    expect(screen.getByText('Test Sidebar Title')).toBeInTheDocument();

    // Close sidebar
    const closeButton = screen.getByRole('button', { name: /close sidebar/i });
    await userEvent.click(closeButton);

    // Sidebar should not be rendered when closed
    expect(screen.queryByRole('complementary')).not.toBeInTheDocument();
  });

  it('should render with correct ARIA attributes', async () => {
    render(
      <TestWrapper>
        <TriggerButton />
        <SidebarWithConsumer />
      </TestWrapper>,
    );

    const openButton = screen.getByRole('button', { name: /open sidebar/i });
    await userEvent.click(openButton);

    const sidebar = screen.getByRole('complementary', { name: /test sidebar title sidebar/i });

    expect(sidebar).toBeInTheDocument();
    expect(screen.getByLabelText('Close sidebar')).toBeInTheDocument();
  });

  it('should render custom component with props', async () => {
    const CustomContent = ({ title, count }: { title: string; count: number }) => (
      <div>
        <h2>{title}</h2>
        <p>Count: {count}</p>
      </div>
    );

    const CustomTrigger = () => {
      const { openSidebar } = useRightSidebar();

      return (
        <button
          type="button"
          onClick={() =>
            openSidebar({
              id: 'custom-sidebar',
              title: 'Custom Sidebar',
              component: CustomContent,
              props: { title: 'Custom Title', count: 42 },
            })
          }>
          Open Custom
        </button>
      );
    };

    render(
      <TestWrapper>
        <CustomTrigger />
        <SidebarWithConsumer />
      </TestWrapper>,
    );

    const openButton = screen.getByRole('button', { name: /open custom/i });
    await userEvent.click(openButton);

    expect(screen.getByText('Custom Title')).toBeInTheDocument();
    expect(screen.getByText('Count: 42')).toBeInTheDocument();
  });

  it('should update content when sidebar is already open', async () => {
    const UpdateButton = () => {
      const { openSidebar, updateContent } = useRightSidebar();

      return (
        <>
          <button
            type="button"
            onClick={() =>
              openSidebar({
                id: 'test',
                title: 'Initial Title',
                component: TestSidebarContent,
                props: { message: 'Initial message' },
              })
            }>
            Open
          </button>
          <button
            type="button"
            onClick={() =>
              updateContent({
                id: 'test',
                title: 'Updated Title',
                component: TestSidebarContent,
                props: { message: 'Updated message' },
              })
            }>
            Update
          </button>
        </>
      );
    };

    render(
      <TestWrapper>
        <UpdateButton />
        <SidebarWithConsumer />
      </TestWrapper>,
    );

    // Open sidebar
    await userEvent.click(screen.getByRole('button', { name: /^open$/i }));
    expect(screen.getByText('Initial Title')).toBeInTheDocument();
    expect(screen.getByText('Initial message')).toBeInTheDocument();

    // Update content
    await userEvent.click(screen.getByRole('button', { name: /update/i }));
    expect(screen.getByText('Updated Title')).toBeInTheDocument();
    expect(screen.getByText('Updated message')).toBeInTheDocument();
  });
});
