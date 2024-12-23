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
import { useMemo, useState } from 'react';

import type { EventDefinition } from 'components/event-definitions/event-definitions-types';

import BulkActions from './BulkActions';

const useTableElements = () => {
  const [selectedEntitiesData, setSelectedEntitiesData] = useState<{[eventDefinitionId: string]: EventDefinition}>({});
  const bulkSelection = useMemo(() => ({
    onChangeSelection: (selectedItemsIds: Array<string>, list: Array<EventDefinition>) => {
      setSelectedEntitiesData((cur) => {
        const uniqueSelectedIds = [...new Set(selectedItemsIds)];
        const eventDefinitionsMap = Object.fromEntries(list.map((event) => [event.id, event]));

        return Object.fromEntries(uniqueSelectedIds.map((selectedItemId) => [selectedItemId, cur[selectedItemId] ?? eventDefinitionsMap[selectedItemId]]));
      });
    },
    actions: <BulkActions />,

  }), [selectedEntitiesData]);

  return {
    bulkActions: <BulkActions />,
    bulkSelection,
  };
};

export default useTableElements;
