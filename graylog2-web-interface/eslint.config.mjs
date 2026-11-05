import { defineConfig } from 'eslint/config';
import graylog from 'eslint-config-graylog';

export default defineConfig([
  {
    extends: [graylog],
  },
  {
    files: ['docs/graylog-luma/**'],
    rules: {
      'graylog/no-literal-product-name': 'off',
    },
  },
]);
