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
import type { Preview } from '@storybook/react-webpack5';

import { withGraylogTheme } from './withGraylogTheme';
import { lightTheme, darkTheme } from './storybook-theme';
import { DocsContainer } from './DocsContainer';

export const decorators = [withGraylogTheme];

const preview: Preview = {
  parameters: {
    options: {
      storySort: {
        order: ['Foundation', ['Typography', 'Spacings'], 'Components'],
      },
    },
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
    docs: {
      container: DocsContainer,
    },
  },
};

export default preview;
