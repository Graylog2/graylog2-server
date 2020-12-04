// We can't use relative paths here, because they do not get resolved properly
// when importing core components which are using theme in plugins.
import type { Colors } from 'src/theme/colors';
import type { Fonts } from 'src/theme/fonts';
import type { Utils } from 'src/theme/utils';
import type { Breakpoints } from 'src/theme/breakpoints';

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
