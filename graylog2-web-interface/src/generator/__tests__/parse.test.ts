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
import { parseApi } from 'generator/parse';
import readJsonFixture from 'helpers/readJsonFixture';

describe('parse', () => {
  it('parses an api properly', () => {
    const rawApi = readJsonFixture(__dirname, './sample-api.json');

    const result = parseApi('sample', rawApi);

    expect(result).toMatchSnapshot();
  });

  it('parses operation with query parameter', () => {
    const api = {
      models: [] as Record<string, any>,
      apis: [
        {
          path: '/system/grok',
          operations: [
            {
              summary: 'Add a list of new patterns',
              notes: '',
              method: 'POST' as const,
              nickname: 'bulkUpdatePatternsFromTextFile',
              produces: ['application/json' as const],
              type: 'any',
              parameters: [
                {
                  paramType: 'query' as const,
                  name: 'import-strategy',
                  description: 'Strategy to apply when importing.',
                  type: 'string',
                  required: false,
                  enum: ['ABORT_ON_CONFLICT', 'REPLACE_ON_CONFLICT', 'DROP_ALL_EXISTING'],
                },
              ],
              responseMessages: [],
            },
          ],
        },
      ],
    };

    const result = parseApi('grok', api);

    expect(result).toMatchSnapshot();
  });

  it('parses arrays of referenced types', () => {
    const api = {
      models: {
        InputStateSummaryArray: {
          type: 'array',
          id: 'InputStateSummaryArray',
          properties: {},
          items: {
            $ref: 'InputStateSummary',
          },
        },
      },
      apis: [],
    };

    const result = parseApi('sample', api);

    expect(result).toMatchInlineSnapshot(`
{
  "description": undefined,
  "models": {
    "InputStateSummaryArray": {
      "id": "InputStateSummaryArray",
      "items": {
        "name": "InputStateSummary",
        "optional": false,
        "type": "type_reference",
      },
      "type": "array",
    },
  },
  "name": "sample",
  "routes": [],
}
`);
  });

  it('parses additional properties', () => {
    const api = {
      apis: [],
      models: {
        AvailableOutputSummaryMapMap: {
          type: 'object',
          id: 'AvailableOutputSummaryMapMap',
          properties: {},
          additional_properties: {
            $ref: 'AvailableOutputSummaryMap',
          },
        },
      },
    };

    const result = parseApi('sample', api);

    expect(result).toMatchInlineSnapshot(`
{
  "description": undefined,
  "models": {
    "AvailableOutputSummaryMapMap": {
      "additionalProperties": {
        "name": "AvailableOutputSummaryMap",
        "optional": false,
        "type": "type_reference",
      },
      "id": "AvailableOutputSummaryMapMap",
      "properties": {},
      "type": "type_literal",
    },
  },
  "name": "sample",
  "routes": [],
}
`);
  });

  it('marks properties that are not required as optional', () => {
    const api = {
      apis: [],
      models: {
        OptionalProperties: {
          type: 'object',
          id: 'OptionalProperties',
          properties: {
            required_value: { type: 'string' },
            optional_value: { type: 'string', required: false },
            optional_entity: { $ref: 'SampleEntity', required: false },
            optional_list: { type: 'array', items: { type: 'string' }, required: false },
          },
        },
      },
    };

    const result = parseApi('sample', api);

    expect(result.models.OptionalProperties).toMatchObject({
      properties: {
        required_value: { type: 'type_reference', name: 'string', optional: false },
        optional_value: { type: 'type_reference', name: 'string', optional: true },
        optional_entity: { type: 'type_reference', name: 'SampleEntity', optional: true },
        optional_list: { type: 'array', optional: true },
      },
    });
  });
});
