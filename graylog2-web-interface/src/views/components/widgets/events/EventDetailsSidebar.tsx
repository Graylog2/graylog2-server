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

import { Spinner } from 'components/common';
import { isPermitted } from 'util/PermissionsMixin';
import useCurrentUser from 'hooks/useCurrentUser';
import useEventById from 'hooks/useEventById';
import useEventDefinition from 'components/events/events/hooks/useEventDefinition';
import usePluginEntities from 'hooks/usePluginEntities';
import GeneralEventDetailsTable from 'components/events/events/GeneralEventDetailsTable';
import { detailsAttributes } from 'components/events/Constants';
import useEventAction from 'components/events/events/hooks/useEventAction';
import DropdownButton from 'components/bootstrap/DropdownButton';
import type { EventDetailsSidebarPlugin } from 'views/types';

const attributesList = detailsAttributes.map(({ id, title }) => ({ id, title }));

type Props = {
  eventId: string;
  hideEditButtons?: boolean;
};

const EventDetailsSidebar = ({ eventId, hideEditButtons = false }: Props) => {
  const pluggableComponents = usePluginEntities('views.components.widgets.events.sidebarComponent');
  const pluggable = pluggableComponents
    .filter((p: EventDetailsSidebarPlugin) => (p.useCondition ? p.useCondition() : true));

  if (pluggable.length > 0) {
    const { component: PluggableComponent } = pluggable[0];

    return <PluggableComponent eventId={eventId} hideEditButtons={hideEditButtons} />;
  }

  return <DefaultEventDetailsSidebar eventId={eventId} />;
};

const DefaultEventDetailsSidebar = ({ eventId }: Props) => {
  const { data: event, isLoading: isLoadingEvent } = useEventById(eventId);
  const currentUser = useCurrentUser();
  const canViewDefinition = isPermitted(currentUser.permissions, `eventdefinitions:read:${event?.event_definition_id}`);
  const { data: eventDefinition, isFetching: isLoadingDefinition } = useEventDefinition(event?.event_definition_id, canViewDefinition);
  const { moreActions, pluggableActionModals } = useEventAction(event);

  const meta = useMemo(
    () => ({ context: { event_definitions: { [event?.event_definition_id]: eventDefinition } } }),
    [event?.event_definition_id, eventDefinition],
  );

  if (isLoadingEvent || isLoadingDefinition) {
    return <Spinner />;
  }

  return (
    <div>
      <GeneralEventDetailsTable attributesList={attributesList} event={event} meta={meta} />
      <DropdownButton title="Actions" buttonTitle="Actions">
        {moreActions}
      </DropdownButton>
      {pluggableActionModals}
    </div>
  );
};

export default EventDetailsSidebar;
