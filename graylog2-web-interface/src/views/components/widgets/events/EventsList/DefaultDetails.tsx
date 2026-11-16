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
import styled from 'styled-components';

import type { Event, EventDefinitionContext } from 'components/events/events/types';
import GeneralEventDetailsTable from 'components/events/events/GeneralEventDetailsTable';
import { detailsAttributes } from 'components/events/Constants';
import sortEventDetailsAttributes from 'components/events/events/sortEventDetailsAttributes';
import DropdownButton from 'components/bootstrap/DropdownButton';
import useEventAction from 'components/events/events/hooks/useEventAction';
import { useIsInteractiveMode } from 'views/components/contexts/InteractiveContext';

type Props = {
  event: Event;
  eventDefinitionContext: EventDefinitionContext;
};

const attributesList = sortEventDetailsAttributes([
  ...detailsAttributes.map(({ id, title }) => ({ id, title })),
  { id: 'tags', title: 'Tags' },
]);

const ActionsWrapperContainer = styled.div`
  margin-top: ${({ theme }) => theme.spacings.md};
`;

const ActionsWrapper = ({ children }) => (
  <ActionsWrapperContainer>
    <DropdownButton title="Actions" buttonTitle="Actions">
      {children}
    </DropdownButton>
  </ActionsWrapperContainer>
);

const DefaultDetails = ({ event, eventDefinitionContext }: Props) => {
  const { moreActions, pluggableActionModals } = useEventAction(event);
  const interactive = useIsInteractiveMode();
  const meta = useMemo(
    () => ({ context: { event_definitions: { [event.event_definition_id]: eventDefinitionContext } } }),
    [event.event_definition_id, eventDefinitionContext],
  );

  return (
    <>
      {/* No filter component is in scope for the events overview widget, so tag chips
          here would just append an unused `filters` query param when clicked. */}
      <GeneralEventDetailsTable attributesList={attributesList} event={event} meta={meta} interactiveTags={false} />
      {interactive && <ActionsWrapper>{moreActions}</ActionsWrapper>}
      {pluggableActionModals}
    </>
  );
};

export default DefaultDetails;
