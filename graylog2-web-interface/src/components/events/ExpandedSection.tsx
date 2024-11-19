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
import React, { useMemo } from 'react';

import useTableLayout from 'components/common/EntityDataTable/hooks/useTableLayout';
import { useTableFetchContext } from 'components/common/PaginatedEntityTable';
import type { Attribute } from 'stores/PaginationTypes';
import type { Event, EventsAdditionalData } from 'components/events/events/types';
import useMetaDataContext from 'components/common/EntityDataTable/hooks/useMetaDataContext';
import EventDetailsTable from 'components/events/events/EventDetailsTable';

type Props = {
  defaultLayout: Parameters<typeof useTableLayout>[0],
  event: Event,
}

const ExpandedSection = ({ defaultLayout, event }: Props) => {
  const { meta } = useMetaDataContext<EventsAdditionalData>();
  const { layoutConfig: { displayedAttributes }, isInitialLoading } = useTableLayout(defaultLayout);
  const { attributes } = useTableFetchContext();

  const nonDisplayedAttributes = useMemo(() => {
    if (isInitialLoading) return [];

    const displayedAttributesSet = new Set(displayedAttributes);

    return attributes.filter(({ id }) => !displayedAttributesSet.has(id)).map(({ id, title }: Attribute) => ({ id, title }));
  }, [attributes, displayedAttributes, isInitialLoading]);

  if (!nonDisplayedAttributes.length) return <em>No further details</em>;

  return <EventDetailsTable attributesList={nonDisplayedAttributes} event={event} meta={meta} />;
};

export default ExpandedSection;
