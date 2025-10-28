import * as React from 'react';
import '@graylog/sawmill/fonts';
import '@mantine/core/styles.css';
import '@mantine/dropzone/styles.css';
import '@mantine/notifications/styles.css';
import type { Decorator } from '@storybook/react';

import GraylogThemeProvider from '../src/theme/GraylogThemeProvider';
import GlobalThemeStyles from '../src/theme/GlobalThemeStyles';
import Notifications from '../src/routing/Notifications';

// eslint-disable-next-line import/prefer-default-export
export const withGraylogTheme: Decorator = (Story) => (
  <GraylogThemeProvider userIsLoggedIn={true}>
    <GlobalThemeStyles />
    <Notifications />
    <Story />
  </GraylogThemeProvider>
);
