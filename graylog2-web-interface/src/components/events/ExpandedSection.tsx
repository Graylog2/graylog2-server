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
import useNonDisplayedAttributes from 'components/events/events/hooks/useNonDisplayedAttributes';
import type { DefaultLayout } from 'components/common/EntityDataTable/types';
import { isPermitted } from 'util/PermissionsMixin';
import useCurrentUser from 'hooks/useCurrentUser';
import TagsDetailRow from 'components/events/TagsDetailRow';

const noDetails = <em>No further details</em>;

type Props = {
  defaultLayout: DefaultLayout;
  event: Event;
};

const TacticsTechniquesPluginRow = ({ event }: { event: Event }) => {
  const plugin = PluginStore.exports('events.components.tacticsTechniquesDetailRow')[0];
  const enabled = plugin?.useCondition?.() ?? !!plugin;
  if (!plugin || !enabled) return null;
  const Row = plugin.component;

  return <Row entity={event} />;
};

const GeneralEventDetails = ({ defaultLayout, event }: Props) => {
  const { meta } = useMetaDataContext<EventsAdditionalData>();
  const nonDisplayedAttributes = useNonDisplayedAttributes(defaultLayout);
  const hasTags = !!event.tags?.length;

  if (!nonDisplayedAttributes.length && !hasTags) return noDetails;

  return (
    <>
      {nonDisplayedAttributes.length ? (
        <GeneralEventDetailsTable attributesList={nonDisplayedAttributes} event={event} meta={meta} />
      ) : null}
      <TagsDetailRow tags={event.tags} />
      <TacticsTechniquesPluginRow event={event} />
    </>
  );
};

const ExpandedSection = ({ defaultLayout, event }: Props) => {
  const { permissions } = useCurrentUser();
  if (!isPermitted(permissions, `eventdefinitions:read:${event.event_definition_id}`)) {
    return noDetails;
  }

  return <GeneralEventDetails defaultLayout={defaultLayout} event={event} />;
};

export default ExpandedSection;
