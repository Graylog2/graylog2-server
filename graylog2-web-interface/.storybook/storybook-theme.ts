import { create } from '@storybook/theming';

const baseTheme = {
  brandTitle: 'Graylog Design System',
  brandUrl: 'https://graylog.org/',
  brandTarget: '_block',
  brandImage: '../src/assets/graylog-logo.png',

  fontBase: '"Poppins", sans-serif',
  fontCode: '"Roboto Mono", monospace',

  colorPrimary: '#03C2FF',
  colorSecondary: '#9A6BFE',

  appBorderRadius: 4,
  inputBorderRadius: 2,
};

export const lightTheme = create({
  base: 'light',
  ...baseTheme,
  appBg: '#F6F7FC',
  appContentBg: '#FFFFFF',
  appPreviewBg: '#F6F7FC',
  appBorderColor: '#E1E4ED',

  textColor: '#252D47',
  textInverseColor: '#252D478A',

  barTextColor: '#252D47',
  barSelectedColor: '#03C2FF',
  barHoverColor: '#03C2FF',
  barBg: '#F6F7FC',

  inputBg: '#F6F7FC',
  inputBorder: '#12182B',
  inputTextColor: '#252D47',
});

export const darkTheme = create({
  base: 'dark',
  ...baseTheme,
  appBg: '#12182B',
  appContentBg: '#252D47',
  appPreviewBg: '#12182B',
  appBorderColor: '#1C2235',

  textColor: '#E1E4ED',
  textInverseColor: '#E1E4ED8A',

  barTextColor: '#E1E4ED',
  barSelectedColor: '#03C2FF',
  barHoverColor: '#03C2FF',
  barBg: '#12182B',

  inputBg: '#12182B',
  inputBorder: '#394261',
  inputTextColor: '#E1E4ED',
});
