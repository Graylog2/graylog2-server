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
import { PluginStore } from 'graylog-web-plugin/plugin';

import type { Event, EventsAdditionalData } from 'components/events/events/types';
import useMetaDataContext from 'components/common/EntityDataTable/hooks/useMetaDataContext';
import GeneralEventDetailsTable from 'components/events/events/GeneralEventDetailsTable';
import { useTableFetchContext } from 'components/common/PaginatedEntityTable';
import usePermissions from 'hooks/usePermissions';
import TagsDetailRow from 'components/events/TagsDetailRow';

const noDetails = <em>No further details</em>;

type Props = {
  event: Event;
};

const TacticsTechniquesPluginRow = ({ event }: { event: Event }) => {
  const plugin = PluginStore.exports('events.components.tacticsTechniquesDetailRow')[0];
  const enabled = plugin?.useCondition?.() ?? !!plugin;
  if (!plugin || !enabled) return null;
  const Row = plugin.component;

  return <Row entity={event} />;
};

const GeneralEventDetails = ({ event }: Props) => {
  const { meta } = useMetaDataContext<EventsAdditionalData>();
  const { attributes } = useTableFetchContext();
  // The details show all attributes, also the ones shown as table columns. The tags follow in their own row.
  const detailsAttributes = attributes.filter(({ id }) => id !== 'tags');
  const hasTags = !!event.tags?.length;

  if (!detailsAttributes.length && !hasTags) return noDetails;

  return (
    <>
      {detailsAttributes.length ? (
        <GeneralEventDetailsTable attributesList={detailsAttributes} event={event} meta={meta} />
      ) : null}
      <TagsDetailRow tags={event.tags} />
      <TacticsTechniquesPluginRow event={event} />
    </>
  );
};

const ExpandedSection = ({ event }: Props) => {
  const { isPermitted } = usePermissions();
  if (!isPermitted(`eventdefinitions:read:${event.event_definition_id}`)) {
    return noDetails;
  }

  return <GeneralEventDetails event={event} />;
};

export default ExpandedSection;
