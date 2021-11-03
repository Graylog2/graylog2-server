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
import * as fs from 'fs';
import * as path from 'path';

import { parseApi } from 'generator/parse';

describe('parse', () => {
  it('parses an api properly', () => {
    const rawApi = JSON.parse(fs.readFileSync(path.resolve(__dirname, './sample-api.json')).toString());

    const result = parseApi('sample', rawApi);

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
Object {
  "description": undefined,
  "models": Object {
    "InputStateSummaryArray": Object {
      "id": "InputStateSummaryArray",
      "items": Object {
        "name": "InputStateSummary",
        "optional": false,
        "type": "type_reference",
      },
      "type": "array",
    },
  },
  "name": "sample",
  "routes": Array [],
}
`);
  });
});
