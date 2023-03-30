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

import useColumnsWidths from './useColumnsWidths';

describe('useColumnsWidths hook test', () => {
  it('should calculate width for columns with flexible width', async () => {
    const columnRenderersByAttribute = {
      title: { width: 1 },
      description: { width: 2 },
    };
    const columnsIds = ['title', 'description'];

    const { result } = renderHook(() => useColumnsWidths({
      columnRenderersByAttribute,
      columnsIds,
      actionsColWidth: 0,
      bulkSelectColWidth: 0,
      tableWidth: 600,
    }));

    expect(result.current).toEqual({
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
    const columnsIds = ['title', 'description'];

    const { result } = renderHook(() => useColumnsWidths({
      columnRenderersByAttribute,
      columnsIds,
      actionsColWidth: 0,
      bulkSelectColWidth: 0,
      tableWidth: 600,
    }));

    expect(result.current).toEqual({
      description: 300,
      title: 300,
    });
  });

  it('should consider width of bulk select and actions col', async () => {
    const columnRenderersByAttribute = {
      title: { width: 1 },
      description: { width: 2 },
    };
    const columnsIds = ['title', 'description'];

    const { result } = renderHook(() => useColumnsWidths({
      columnRenderersByAttribute,
      columnsIds,
      actionsColWidth: 50,
      bulkSelectColWidth: 20,
      tableWidth: 600,
    }));

    expect(result.current).toEqual({
      description: 353.3333333333333,
      title: 176.66666666666666,
    });
  });
});
