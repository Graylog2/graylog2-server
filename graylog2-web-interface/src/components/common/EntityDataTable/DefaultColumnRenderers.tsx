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

import React from 'react';

import TextOverflowEllipsis from 'components/common/TextOverflowEllipsis';
import { Timestamp } from 'components/common';

const DefaultColumnRenderers = {
  types: {
    DATE: {
      renderCell: (dateTime: string) => (
        <Timestamp dateTime={dateTime} />
      ),
      staticWidth: 160,
    },
    STRING: {
      renderCell: (text: string) => (
        <TextOverflowEllipsis>
          {text}
        </TextOverflowEllipsis>
      ),
    },
  },
  attributes: {
    description: {
      width: 2,
    },
    summary: {
      width: 1.5,
    },
    owner: {
      staticWidth: 120,
    },
    favorite: {
      renderHeader: () => '',
      staticWidth: 30,
    },
  },

};

export default DefaultColumnRenderers;
