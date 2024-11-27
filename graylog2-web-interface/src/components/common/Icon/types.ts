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

import type { MaterialSymbol as IconName } from 'material-symbols';

export type SizeProp = 'xs' | 'sm' | 'lg' | 'xl' | '2x' | '3x' | '4x' | '5x'
export type RotateProp = 0 | 90 | 80 | 270;
export type FlipProp = 'horizontal' | 'vertical' | 'both';
export type IconType = 'regular' | 'solid';

export { IconName };
