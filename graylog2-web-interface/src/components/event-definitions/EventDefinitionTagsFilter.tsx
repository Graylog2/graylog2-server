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
import { EventsDefinitions } from '@graylog/server-api';

import { createTagsFilter } from 'components/events/TagsFilter';

const SUGGESTION_LIMIT = 100;

const fetchSuggestions = () =>
  EventsDefinitions.suggestTags('', SUGGESTION_LIMIT).then((response) =>
    (response?.tags ?? []).sort().map((value: string) => ({ id: value, value })),
  );

const EventDefinitionTagsFilter = createTagsFilter({
  queryKeyPrefix: ['event-definitions', 'tag-filter-suggestions'],
  fetchSuggestions,
});

export default EventDefinitionTagsFilter;
