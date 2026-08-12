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

import { CountBadge } from 'components/common';
import useExpandedSections from 'components/common/EntityDataTable/hooks/useExpandedSections';

import type { EventDefinition } from '../event-definitions-types';

type Props = {
  eventDefinition: EventDefinition;
};

const EventDefinitionNotificationsCell = ({ eventDefinition }: Props) => {
  const { toggleSection, expandedSections } = useExpandedSections();
  const count = eventDefinition.notifications.length;

  if (count === 0) return null;

  const isOpen = expandedSections?.[eventDefinition.id]?.includes('notifications');
  const title = `${isOpen ? 'Hide' : 'Show'} notifications`;

  return (
    <CountBadge
      count={count}
      iconName={isOpen ? 'keyboard_arrow_up' : 'keyboard_arrow_down'}
      onClick={() => toggleSection(eventDefinition.id, 'notifications')}
      title={title}
    />
  );
};

export default EventDefinitionNotificationsCell;
