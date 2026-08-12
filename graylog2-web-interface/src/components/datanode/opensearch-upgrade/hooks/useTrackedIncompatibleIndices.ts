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
import { useState } from 'react';
import keyBy from 'lodash/keyBy';
import pickBy from 'lodash/pickBy';

import type { IncompatibleIndexRow, IncompatibleIndicesResponse } from '../fetchIncompatibleIndices';

type TrackedIncompatibleIndices = {
  selectedIndices: Array<IncompatibleIndexRow>;
  trackedIndices: Array<IncompatibleIndexRow>;
  hasLoaded: boolean;
  onDataLoaded: (data: IncompatibleIndicesResponse) => void;
  onChangeSelection: (selectedItemsIds: Array<string>, list: Readonly<Array<IncompatibleIndexRow>>) => void;
};

const useTrackedIncompatibleIndices = (): TrackedIncompatibleIndices => {
  const [loadedIndices, setLoadedIndices] = useState<Array<IncompatibleIndexRow>>([]);
  const [selectedIndicesData, setSelectedIndicesData] = useState<Record<string, IncompatibleIndexRow>>({});
  const [hasLoaded, setHasLoaded] = useState(false);

  const selectedIndices = Object.values(selectedIndicesData);
  const trackedIndices = Object.values({ ...selectedIndicesData, ...keyBy(loadedIndices, 'id') });

  const onDataLoaded = (data: IncompatibleIndicesResponse) => {
    setLoadedIndices(data.list);
    setHasLoaded(true);
    setSelectedIndicesData((cur) => ({
      ...cur,
      ...keyBy(
        data.list.filter(({ id }) => Object.hasOwn(cur, id)),
        'id',
      ),
    }));
  };

  const onChangeSelection = (selectedItemsIds: Array<string>, list: Readonly<Array<IncompatibleIndexRow>>) => {
    setSelectedIndicesData((cur) => {
      const selectedItemsIdsSet = new Set(selectedItemsIds);
      const selectedCurrentItems = pickBy(cur, (_, indexId) => selectedItemsIdsSet.has(indexId));
      const currentEntriesById = keyBy(
        list.filter(({ id }) => selectedItemsIdsSet.has(id)),
        'id',
      );

      return { ...selectedCurrentItems, ...currentEntriesById };
    });
  };

  return { selectedIndices, trackedIndices, hasLoaded, onDataLoaded, onChangeSelection };
};

export default useTrackedIncompatibleIndices;
