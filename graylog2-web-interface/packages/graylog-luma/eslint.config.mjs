import storybook from 'eslint-plugin-storybook';
import { defineConfig } from 'eslint/config';
import graylog from 'eslint-config-graylog';
export default defineConfig([
  ...storybook.configs['flat/recommended'],
  {
    extends: [graylog],
  },
]);
