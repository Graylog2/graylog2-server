import type { Preview } from '@storybook/react-webpack5';

import { withGraylogTheme } from './graylog-theme-decorator';
import { lightTheme, darkTheme } from './storybook-theme';

export const decorators = [withGraylogTheme];

const preview: Preview = {
  parameters: {
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },
    darkMode: {
      dark: darkTheme,
      light: lightTheme,
      current: 'dark',
      stylePreview: true,
    },
  },
};

export default preview;

