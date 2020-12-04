import 'styled-components';

import type { Colors } from './colors';
import type { Fonts } from './fonts';
import type { Utils } from './utils';
import type { Breakpoints } from './breakpoints';

declare module 'styled-components' {
  export interface DefaultTheme {
    breakpoints: Breakpoints,
    colors: Colors,
    fonts: Fonts,
    utils: Utils,
    mode: string,
    changeMode: (string) => void,
    components: { [component: string]: any },
  }
}
