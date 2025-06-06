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
import * as React from 'react';
import { useContext } from 'react';

import type { EventDefinition } from 'components/event-definitions/event-definitions-types';
import StreamsContext from 'contexts/StreamsContext';
import FilterPreviewResults from 'components/event-definitions/event-definition-types/FilterPreviewResults';

import FilterPreview from './FilterPreview';

type FilterPreviewContainerProps = {
  eventDefinition: EventDefinition;
};
const FilterPreviewContainer = ({ eventDefinition }: FilterPreviewContainerProps) => {
  const streams = useContext(StreamsContext);

  return streams?.length ? (
    <FilterPreview config={eventDefinition?.config} />
  ) : (
    <FilterPreviewResults>Unable to preview filter, user does not have access to any streams.</FilterPreviewResults>
  );
};

export default FilterPreviewContainer;
