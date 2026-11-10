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

import { DEFAULT_COL_WIDTH } from 'components/common/EntityDataTable/Constants';

import useColumnWidths from './useColumnWidths';

describe('useColumnWidths hook test', () => {
  const defaultProps = {
    actionsColMinWidth: 0,
    bulkSelectColWidth: 0,
    columnWidthPreferences: undefined,
    scrollContainerWidth: 600,
    headerMinWidths: { title: 100, description: 110 },
    columnIds: ['title', 'description'],
  };

  it('should use static widths and fill the remaining width with the actions column', async () => {
    const columnRenderersByAttribute = {
      title: { staticWidth: 300 },
      description: { staticWidth: 200 },
    };

    const { result } = renderHook(() =>
      useColumnWidths({
        ...defaultProps,
        columnRenderersByAttribute,
      }),
    );

    expect(result.current).toEqual({
      actions: 99,
      description: 200,
      title: 300,
    });
  });

  it('should use default width for columns without static width', async () => {
    const columnRenderersByAttribute = {
      title: { staticWidth: 300 },
    };

    const { result } = renderHook(() =>
      useColumnWidths({
        ...defaultProps,
        scrollContainerWidth: 1000,
        columnRenderersByAttribute,
      }),
    );

    expect(result.current).toEqual({
      actions: 499,
      description: DEFAULT_COL_WIDTH,
      title: 300,
    });
  });

  it('should prefer column width preferences over static widths', async () => {
    const columnRenderersByAttribute = {
      title: { staticWidth: 300 },
      description: { staticWidth: 200 },
    };

    const { result } = renderHook(() =>
      useColumnWidths({
        ...defaultProps,
        columnRenderersByAttribute,
        columnWidthPreferences: { title: 250 },
      }),
    );

    expect(result.current).toEqual({
      actions: 149,
      description: 200,
      title: 250,
    });
  });

  it('should keep column widths and use actions column min width when there is no remaining width', async () => {
    const columnRenderersByAttribute = {
      title: { staticWidth: 300 },
      description: { staticWidth: 300 },
    };

    const { result } = renderHook(() =>
      useColumnWidths({
        ...defaultProps,
        actionsColMinWidth: 110,
        bulkSelectColWidth: 20,
        columnRenderersByAttribute,
      }),
    );

    expect(result.current).toEqual({
      actions: 110,
      'bulk-select': 20,
      description: 300,
      title: 300,
    });
  });

  it('should consider header min widths', async () => {
    const columnRenderersByAttribute = {
      title: { staticWidth: 'matchHeader' as const },
      description: { staticWidth: 100 },
    };

    const { result } = renderHook(() =>
      useColumnWidths({
        ...defaultProps,
        columnRenderersByAttribute,
        headerMinWidths: { title: 120, description: 150 },
      }),
    );

    expect(result.current).toEqual({
      actions: 329,
      description: 150,
      title: 120,
    });
  });

  it('should use default width for columns matching the header width until the header has been measured', async () => {
    const columnRenderersByAttribute = {
      title: { staticWidth: 'matchHeader' as const },
      description: { staticWidth: 100 },
    };

    const { result } = renderHook(() =>
      useColumnWidths({
        ...defaultProps,
        columnRenderersByAttribute,
        headerMinWidths: {},
      }),
    );

    expect(result.current).toEqual({
      actions: 299,
      description: 100,
      title: DEFAULT_COL_WIDTH,
    });
  });
});
