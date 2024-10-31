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
import EventsWidget from 'views/logic/widgets/events/EventsWidget';

import EventsListSortConfig from './EventsListSortConfig';

const EventsListConfigGenerator = ({ config: { filters, sort } }: EventsWidget) => [{
  sort: new EventsListSortConfig(sort.field, sort.direction),
  type: EventsWidget.type,
  attributes: filters,
  page: 1,
  per_page: 10,
}];

export default EventsListConfigGenerator;
