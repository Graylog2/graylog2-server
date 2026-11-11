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
import emit from 'generator/emit';

describe('emit', () => {
  it('generates stubs for query parameters properly', () => {
    const api = {
      name: 'grok',
      routes: [
        {
          path: '/system/grok',
          operations: [
            {
              method: 'POST' as const,
              summary: 'Add a list of new patterns',
              nickname: 'bulkUpdatePatternsFromTextFile',
              type: {
                type: 'type_reference' as const,
                name: 'unknown',
                optional: false,
              },
              parameters: [
                {
                  name: 'import-strategy',
                  description: 'Strategy to apply when importing.',
                  paramType: 'query' as const,
                  required: false,
                  type: {
                    type: 'enum' as const,
                    name: 'string' as const,
                    options: ['ABORT_ON_CONFLICT', 'REPLACE_ON_CONFLICT', 'DROP_ALL_EXISTING'],
                  },
                },
              ],
              produces: ['application/json' as const],
            },
          ],
        },
      ],
      models: {},
    };

    const result = emit([api]);

    expect(result).toMatchInlineSnapshot(`
[
  {
    "grok": "import __request__ from 'routing/request';
import type { RequestOptions } from 'routing/request';
/**
 * Add a list of new patterns
 * @param import-strategy Strategy to apply when importing.
 */
export function bulkUpdatePatternsFromTextFile(importStrategy?: 'ABORT_ON_CONFLICT' | 'REPLACE_ON_CONFLICT' | 'DROP_ALL_EXISTING', requestOptions?: RequestOptions): Promise<unknown> {
    return __request__('POST', '/system/grok', null, { 'import-strategy': importStrategy }, {
        'Accept': ['application/json']
    }, requestOptions);
}
",
  },
  "export * as grok from './grok';
",
]
`);
  });

  it('sends header parameters as request headers', () => {
    const api = {
      name: 'cells',
      routes: [
        {
          path: '/cells/{cellId}',
          operations: [
            {
              method: 'PUT' as const,
              summary: 'Update a cell',
              nickname: 'updateCell',
              type: {
                type: 'type_reference' as const,
                name: 'unknown',
                optional: false,
              },
              parameters: [
                {
                  name: 'cellId',
                  description: '',
                  paramType: 'path' as const,
                  required: true,
                  type: { type: 'type_reference' as const, name: 'string', optional: false },
                },
                {
                  name: 'If-Match',
                  description: "The cell's etag",
                  paramType: 'header' as const,
                  required: false,
                  type: { type: 'type_reference' as const, name: 'string', optional: false },
                },
              ],
              produces: ['application/json' as const],
            },
          ],
        },
      ],
      models: {},
    };

    const result = emit([api]);

    expect(result).toMatchInlineSnapshot(`
[
  {
    "cells": "import __request__ from 'routing/request';
import type { RequestOptions } from 'routing/request';
/**
 * Update a cell
 * @param If-Match The cell's etag
 */
export function updateCell(cellId: string, IfMatch?: string, requestOptions?: RequestOptions): Promise<unknown> {
    return __request__('PUT', \`/cells/\${cellId}\`, null, {}, {
        'Accept': ['application/json'],
        'If-Match': IfMatch
    }, requestOptions);
}
",
  },
  "export * as cells from './cells';
",
]
`);
  });

  it('emits proper indexer signatures for collection-types', () => {
    const api = {
      name: 'sample',
      routes: [
        {
          path: '/sample',
          operations: [
            {
              method: 'POST' as const,
              summary: 'A sample operation',
              nickname: 'sample',
              type: {
                type: 'type_reference' as const,
                name: 'AvailableOutputSummaryMapMap',
                optional: false,
              },
              parameters: [],
              produces: ['application/json' as const],
            },
          ],
        },
      ],
      models: {
        AvailableOutputSummaryMapMap: {
          type: 'type_literal' as const,
          properties: {},
          additionalProperties: {
            type: 'type_reference' as const,
            name: 'AvailableOutputSummaryMap',
            optional: false,
          },
          id: 'AvailableOutputSummaryMapMap',
        },
      },
    };

    const result = emit([api]);

    expect(result).toMatchInlineSnapshot(`
[
  {
    "sample": "import __request__ from 'routing/request';
import type { RequestOptions } from 'routing/request';
interface AvailableOutputSummaryMapMap {
    readonly [_key: string]: AvailableOutputSummaryMap;
}
/**
 * A sample operation
 */
export function sample(requestOptions?: RequestOptions): Promise<AvailableOutputSummaryMapMap> {
    return __request__('POST', '/sample', null, {}, {
        'Accept': ['application/json']
    }, requestOptions);
}
",
  },
  "export * as sample from './sample';
",
]
`);
  });

  it('fails if a path variable has no parameter', () => {
    const api = {
      name: 'configurations',
      routes: [
        {
          path: '/sidecar/configurations/{id}/{name}',
          operations: [
            {
              method: 'POST' as const,
              summary: 'Copy a configuration',
              nickname: 'copyConfiguration',
              type: { type: 'type_reference' as const, name: 'unknown', optional: false },
              parameters: [
                {
                  name: 'id',
                  description: '',
                  paramType: 'path' as const,
                  required: true,
                  type: { type: 'type_reference' as const, name: 'string', optional: false },
                },
              ],
              produces: ['application/json' as const],
            },
          ],
        },
      ],
      models: {},
    };

    expect(() => emit([api])).toThrow(
      'POST /sidecar/configurations/{id}/{name} (copyConfiguration) has no parameter for: name',
    );
  });
});
