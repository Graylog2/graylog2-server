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
                    options: [
                      'ABORT_ON_CONFLICT',
                      'REPLACE_ON_CONFLICT',
                      'DROP_ALL_EXISTING',
                    ],
                  },
                },
              ],
              produces: [
                'application/json' as const,
              ],
            },
          ],
        },
      ],
      models: {},
    };

    const result = emit([api]);

    expect(result).toMatchInlineSnapshot(`
Array [
  Object {
    "grok": "import __request__ from 'routing/request';
/**
 * Add a list of new patterns
 * @param import-strategy Strategy to apply when importing.
 */
export function bulkUpdatePatternsFromTextFile(importStrategy?: 'ABORT_ON_CONFLICT' | 'REPLACE_ON_CONFLICT' | 'DROP_ALL_EXISTING'): Promise<unknown> {
    return __request__('POST', '/system/grok', null, { 'import-strategy': importStrategy }, {
        'Accept': ['application/json']
    });
}
",
  },
  "export * as grok from './grok';
",
]
`);
  });

  it('emits proper indexer signatures for collection-types', () => {
    const api = {
      name: 'sample',
      routes: [],
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
Array [
  Object {
    "sample": "import __request__ from 'routing/request';
interface AvailableOutputSummaryMapMap {
    readonly [_key: string]: AvailableOutputSummaryMap;
}
",
  },
  "export * as sample from './sample';
",
]
`);
  });
});
