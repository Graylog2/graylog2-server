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
import type SwimlaneWidget from 'views/logic/widgets/SwimlaneWidget';
import MessageSortConfig from 'views/logic/searchtypes/messages/MessageSortConfig';
import Direction from 'views/logic/aggregationbuilder/Direction';
import { TIMESTAMP_FIELD } from 'views/Constants';

const SwimlaneConfigGenerator = (widget: SwimlaneWidget) => {
  const { config: { limit } } = widget;

  return [
    {
      type: 'messages',
      sort: [new MessageSortConfig(TIMESTAMP_FIELD, Direction.Ascending)],
      limit,
      offset: 0,
    },
  ];
};

export default SwimlaneConfigGenerator;
