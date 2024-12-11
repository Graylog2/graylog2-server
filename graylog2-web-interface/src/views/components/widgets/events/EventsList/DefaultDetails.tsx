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

import type { Event, EventDefinitionContext } from 'components/events/events/types';
import GeneralEventDetailsTable from 'components/events/events/GeneralEventDetailsTable';
import { detailsAttributes } from 'components/events/Constants';
import DropdownButton from 'components/bootstrap/DropdownButton';
import useEventAction from 'components/events/events/hooks/useEventAction';

type Props = {
  event: Event,
  eventDefinitionContext: EventDefinitionContext,
};

const attributesList = detailsAttributes.map(({ id, title }) => ({ id, title }));

const ActionsWrapper = ({ children }) => (
  <DropdownButton title="Actions"
                  buttonTitle="Actions">
    {children}
  </DropdownButton>
);

const DefaultDetails = ({ event, eventDefinitionContext }: Props) => {
  const { moreActions, pluggableActionModals } = useEventAction(event);
  const meta = useMemo(() => ({ context: { event_definitions: { [event.event_definition_id]: eventDefinitionContext } } }), [event.event_definition_id, eventDefinitionContext]);

  return (
    <>
      <GeneralEventDetailsTable attributesList={attributesList} event={event} meta={meta} />
      <ActionsWrapper>
        {moreActions}
      </ActionsWrapper>
      {pluggableActionModals}
    </>
  );
};

export default DefaultDetails;
