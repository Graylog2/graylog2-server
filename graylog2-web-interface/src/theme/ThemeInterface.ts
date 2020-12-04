import type { Colors } from './colors';
import type { Fonts } from './fonts';
import type { Utils } from './utils';
import type { Breakpoints } from './breakpoints';

// This interface is being used in the styled-component type declaration and can be imported with
// import type { DefaultTheme } from 'styled-components';
export interface ThemeInterface {
  breakpoints: Breakpoints,
  colors: Colors,
  fonts: Fonts,
  utils: Utils,
  mode: string,
  changeMode: (string) => void,
  components: { [component: string]: any }
}
