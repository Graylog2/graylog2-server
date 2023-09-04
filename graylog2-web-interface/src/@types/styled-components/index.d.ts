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
import type { SawmillSC } from '@graylog/sawmill';

declare module 'styled-components' {
  // We need to define the theme interface here and can't use relative
  // paths, because otherwise the paths do not get resolved properly
  // when importing core components which are using the theme, in plugins.
  export interface DefaultTheme extends SawmillSC {}
}
