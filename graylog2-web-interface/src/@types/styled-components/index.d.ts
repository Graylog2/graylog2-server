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
import 'styled-components';

declare module 'styled-components' {
  // We need to define the theme interface here and can't use relative
  // paths, because otherwise the paths do not get resolved properly
  // when importing core components which are using the theme, in plugins.
  import type { Colors } from 'src/theme/colors';
  import type { Fonts } from 'src/theme/fonts';
  import type { Utils } from 'src/theme/utils';
  import type { ThemeMode } from 'src/theme/constants';
  import type { Spacings } from 'src/theme/spacings';
  // eslint-disable-next-line import/order
  import type { Breakpoints } from 'src/theme/breakpoints';

  export interface DefaultTheme {
    breakpoints: Breakpoints,
    colors: Colors,
    fonts: Fonts,
    utils: Utils,
    mode: ThemeMode,
    spacings: Spacings,
    changeMode: (string) => void,
    components: { [component: string]: any }
  }
}
