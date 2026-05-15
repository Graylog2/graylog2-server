import { defineConfig } from 'eslint/config';
import { configs } from 'eslint-plugin-storybook';
import graylog from 'eslint-config-graylog';

export default defineConfig([
  {
    extends: [graylog, ...configs['flat/recommended']],
  },
]);
