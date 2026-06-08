import { defineConfig } from 'eslint/config';
import graylog from 'eslint-config-graylog';

export default defineConfig([
  {
    extends: [graylog],
  },
]);
