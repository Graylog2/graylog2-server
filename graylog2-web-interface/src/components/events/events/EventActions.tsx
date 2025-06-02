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

import { ButtonToolbar, Button } from 'components/bootstrap';
import { MoreActions } from 'components/common/EntityDataTable';
import type { Event } from 'components/events/events/types';
import useExpandedSections from 'components/common/EntityDataTable/hooks/useExpandedSections';
import useEventAction from 'components/events/events/hooks/useEventAction';
import useCurrentUser from 'hooks/useCurrentUser';
import { isPermitted } from 'util/PermissionsMixin';

const EventActions = ({ event }: { event: Event }) => {
  const { moreActions, pluggableActionModals } = useEventAction(event);
  const { toggleSection } = useExpandedSections();
  const toggleExtraSection = () => toggleSection(event.id, 'restFieldsExpandedSection');

  const { permissions } = useCurrentUser();
  const canReadEventDefinition = isPermitted(permissions, `eventdefinitions:edit:${event.event_definition_id}`);

  return (
    <>
      <ButtonToolbar>
        <Button bsSize="xs" onClick={toggleExtraSection}>
          Details
        </Button>
        {moreActions.length && canReadEventDefinition ? <MoreActions>{moreActions}</MoreActions> : null}
      </ButtonToolbar>
      {pluggableActionModals}
    </>
  );
};

export default EventActions;
