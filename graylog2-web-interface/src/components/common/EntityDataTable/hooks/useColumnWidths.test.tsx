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
import { renderHook } from 'wrappedTestingLibrary/hooks';

import useColumnWidths from './useColumnWidths';

describe('useColumnWidths hook test', () => {
  const defaultProps = {
    actionsColMinWidth: 0,
    bulkSelectColWidth: 0,
    columnWidthPreferences: undefined,
    scrollContainerWidth: 600,
    headerMinWidths: { title: 100, description: 110 },
    columnSchemas: [
      { id: 'title', title: 'Title' },
      { id: 'description', title: 'Description' },
    ],
  };

  it('should calculate width for columns with flexible width', async () => {
    const columnRenderersByAttribute = {
      title: { width: 1 },
      description: { width: 2 },
    };
    const columnIds = ['title', 'description'];

    const { result } = renderHook(() =>
      useColumnWidths({
        ...defaultProps,
        columnRenderersByAttribute,
        columnIds,
      }),
    );

    expect(result.current).toEqual({
      actions: 0,
      description: 400,
      title: 200,
    });
  });

  it('should use default width for columns without column renderers', async () => {
    const columnRenderersByAttribute = {
      title: {
        width: 1,
      },
    };
    const columnIds = ['title', 'description'];

    const { result } = renderHook(() =>
      useColumnWidths({
        ...defaultProps,
        columnRenderersByAttribute,
        columnIds,
      }),
    );

    expect(result.current).toEqual({
      actions: 0,
      description: 300,
      title: 300,
    });
  });

  it('should consider width of bulk select and actions col', async () => {
    const columnRenderersByAttribute = {
      title: { width: 1 },
      description: { width: 2 },
    };
    const columnIds = ['title', 'description'];

    const { result } = renderHook(() =>
      useColumnWidths({
        ...defaultProps,
        actionsColMinWidth: 110,
        bulkSelectColWidth: 20,
        columnRenderersByAttribute,
        columnIds,
      }),
    );

    expect(result.current).toEqual({
      actions: 110,
      'bulk-select': 20,
      description: 313,
      title: 156,
    });
  });

  it('should use actions column to fill remaining width when all columns are static', async () => {
    const columnRenderersByAttribute = {
      title: { staticWidth: 200 },
      description: { staticWidth: 200 },
    };
    const columnIds = ['title', 'description'];

    const { result } = renderHook(() =>
      useColumnWidths({
        ...defaultProps,
        columnRenderersByAttribute,
        columnIds,
      }),
    );

    expect(result.current).toEqual({
      actions: 200,
      description: 200,
      title: 200,
    });
  });

  it('should consider header min widths', async () => {
    const columnRenderersByAttribute = {
      title: { width: 1 },
      description: { staticWidth: 100 },
    };
    const headerMinWidths = { description: 150 };
    const columnIds = ['title', 'description'];

    const { result } = renderHook(() =>
      useColumnWidths({
        ...defaultProps,
        scrollContainerWidth: 1500,
        actionsColMinWidth: 110,
        bulkSelectColWidth: 20,
        columnRenderersByAttribute,
        columnIds,
        headerMinWidths,
      }),
    );

    expect(result.current).toEqual({
      actions: 110,
      'bulk-select': 20,
      description: 150,
      title: 1220,
    });
  });
});
