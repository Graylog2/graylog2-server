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

import BulkActionsDropdown from 'components/common/EntityDataTable/BulkActionsDropdown';
import type { Event } from 'components/events/events/types';
import useEventBulkActions from 'components/events/events/hooks/useEventBulkActions';

type Props = {
  selectedEntitiesData: {[eventId: string]: Event}
}

const BulkActions = ({ selectedEntitiesData }: Props) => {
  const { actions, pluggableActionModals } = useEventBulkActions(Object.values(selectedEntitiesData));

  return (
    <>
      <BulkActionsDropdown>
        {actions}
      </BulkActionsDropdown>
      {pluggableActionModals}
    </>
  );
};

export default BulkActions;
