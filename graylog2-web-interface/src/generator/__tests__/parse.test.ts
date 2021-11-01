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
