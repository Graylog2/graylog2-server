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

import uniq from 'lodash/uniq';

import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import type { EntityBase } from 'components/common/EntityDataTable/types';
import { keySeparator } from 'views/Constants';
import type { SelectableMessageTableMessage } from 'views/components/widgets/MessageList';

export const createSelectedEntityId = (index: string, id: string) => `${index}${keySeparator}${id}`;
const splitSelectedEntityId = (selectedEntityId: string) => {
  const [index, id] = selectedEntityId.split(keySeparator);

  return { index, id };
};

const useSelectedMessageEntities = () => {
  const { setSelectedEntities, isAllRowsSelected, isSomeRowsSelected, toggleEntitySelect, selectedEntities } =
    useSelectedEntities();

  const _toggleEntitySelect = (index: string, id: EntityBase['id']) =>
    toggleEntitySelect(createSelectedEntityId(index, id));

  const toggleAllEntitySelect = (data: Array<SelectableMessageTableMessage>) => {
    setSelectedEntities((cur) => {
      const entityIds = data.map(({ id, index }) => createSelectedEntityId(index, id));

      if (isAllRowsSelected) {
        return cur.filter((itemId) => !entityIds.includes(itemId));
      }

      return uniq([...cur, ...entityIds]);
    });
  };

  const isEntitySelected = (index: string, id: EntityBase['id']) =>
    selectedEntities.includes(createSelectedEntityId(index, id));

  const _selectedEntities = selectedEntities.map((selectedEntityId) => splitSelectedEntityId(selectedEntityId));

  return {
    toggleAllEntitySelect,
    toggleEntitySelect: _toggleEntitySelect,
    isSomeRowsSelected,
    isEntitySelected,
    selectedEntities: _selectedEntities,
    isAllRowsSelected,
  };
};

export default useSelectedMessageEntities;
