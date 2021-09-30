import * as fs from 'fs';
import * as path from 'path';

import { parseApi } from 'generator/parse';

describe('parse', () => {
  it('parses an api properly', () => {
    const rawApi = JSON.parse(fs.readFileSync(path.resolve(__dirname, './fixtures/sample-api.json')).toString());

    const result = parseApi('sample', rawApi);

    expect(result).toMatchSnapshot();
  });
});
