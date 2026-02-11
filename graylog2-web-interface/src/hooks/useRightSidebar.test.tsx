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
import { renderHook, waitFor } from 'wrappedTestingLibrary/hooks';
import { act } from '@testing-library/react';

import useRightSidebar from 'hooks/useRightSidebar';
import RightSidebarProvider from 'contexts/RightSidebarProvider';

const TestComponent = () => <div>Test</div>;

describe('useRightSidebar', () => {
  const Wrapper = ({ children }: { children: React.ReactNode }) => (
    <RightSidebarProvider>{children}</RightSidebarProvider>
  );

  it('should return context value from RightSidebarProvider', () => {
    const { result } = renderHook(() => useRightSidebar(), { wrapper: Wrapper });

    expect(result.current).toEqual(
      expect.objectContaining({
        isOpen: true,
        content: null,
        width: 400,
        openSidebar: expect.any(Function),
        closeSidebar: expect.any(Function),
        updateContent: expect.any(Function),
        setWidth: expect.any(Function),
      }),
    );
  });

  it('should throw error when used outside of RightSidebarProvider', () => {
    // Suppress console.error for this test since we expect an error
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});

    expect(() => renderHook(() => useRightSidebar())).toThrow(
      'useRightSidebar hook needs to be used inside RightSidebarProvider',
    );

    consoleSpy.mockRestore();
  });

  it('should open sidebar with content', async () => {
    const { result } = renderHook(() => useRightSidebar(), { wrapper: Wrapper });

    const sidebarContent = {
      id: 'test-sidebar',
      title: 'Test Sidebar',
      component: TestComponent,
      props: { testProp: 'value' },
    };

    act(() => {
      result.current.openSidebar(sidebarContent);
    });

    await waitFor(() => {
      expect(result.current.isOpen).toBe(true);
      expect(result.current.content).toEqual(sidebarContent);
    });
  });

  it('should close sidebar', async () => {
    const { result } = renderHook(() => useRightSidebar(), { wrapper: Wrapper });

    // Open sidebar first
    act(() => {
      result.current.openSidebar({
        id: 'test',
        title: 'Test',
        component: TestComponent,
      });
    });

    await waitFor(() => {
      expect(result.current.isOpen).toBe(true);
    });

    // Close sidebar
    act(() => {
      result.current.closeSidebar();
    });

    await waitFor(() => {
      expect(result.current.isOpen).toBe(false);
    });
  });

  it('should update content', async () => {
    const { result } = renderHook(() => useRightSidebar(), { wrapper: Wrapper });

    const initialContent = {
      id: 'initial',
      title: 'Initial',
      component: TestComponent,
    };

    const updatedContent = {
      id: 'updated',
      title: 'Updated',
      component: TestComponent,
      props: { newProp: 'newValue' },
    };

    act(() => {
      result.current.openSidebar(initialContent);
    });

    await waitFor(() => {
      expect(result.current.content).toEqual(initialContent);
    });

    act(() => {
      result.current.updateContent(updatedContent);
    });

    await waitFor(() => {
      expect(result.current.content).toEqual(updatedContent);
    });
  });

  it('should update width', async () => {
    const { result } = renderHook(() => useRightSidebar(), { wrapper: Wrapper });

    expect(result.current.width).toBe(400);

    act(() => {
      result.current.setWidth(600);
    });

    await waitFor(() => {
      expect(result.current.width).toBe(600);
    });
  });
});
