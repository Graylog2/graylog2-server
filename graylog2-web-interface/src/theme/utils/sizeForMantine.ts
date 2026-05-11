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

import type { BsSize } from 'components/bootstrap/types';
import type { SupportedMantineSize } from 'theme/types';

const sizeForMantine = (size: BsSize): SupportedMantineSize => {
  switch (size) {
    case 'xs':
    case 'xsmall':
      return 'xs';
    case 'sm':
    case 'small':
      return 'sm';
    case 'lg':
    case 'large':
      return 'lg';
    default:
      return 'md';
  }
};

export default sizeForMantine;
