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
import { useEffect, useMemo, useState } from 'react';
import keyBy from 'lodash/keyBy';
import pickBy from 'lodash/pickBy';

import type { EntityBase } from 'components/common/EntityDataTable/types';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';

const useSelectedEntitiesData = <Entity extends EntityBase>(data : ReadonlyArray<Entity>):Array<Entity> => {
  const { selectedEntities } = useSelectedEntities();
  const [selectedEntitiesData, setSelectedEntitiesData] = useState<Record<Entity['id'], Entity> | {}>({});
  const normalizedList = useMemo(() => keyBy(data, 'id'), [data]);
  const selectedEntitiesSet = useMemo(() => new Set(selectedEntities), [selectedEntities]);

  useEffect(() => {
    setSelectedEntitiesData((cur) => {
      const newItems = pickBy(cur, ({ id }: Entity) => selectedEntitiesSet.has(id));

      selectedEntities.forEach((id) => {
        if (normalizedList[id]) {
          newItems[id] = normalizedList[id];
        }
      });

      return newItems;
    });
  },
  [normalizedList, selectedEntities, selectedEntitiesSet]);

  return Object.values(selectedEntitiesData);
};

export default useSelectedEntitiesData;
