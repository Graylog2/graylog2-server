// For more info, see https://github.com/storybookjs/eslint-plugin-storybook#configuration-flat-config-format
import storybook from "eslint-plugin-storybook";

import { defineConfig } from 'eslint/config';
import graylog from 'eslint-config-graylog';
export default defineConfig([
  {
    extends: [graylog],
  },
]);
