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
import isEqual from 'lodash/isEqual';

type ColumnWidthPreferences = {
  [attributeId: string]: number;
};

type LayoutPreferences = {
  order?: Array<string>;
  attributes?: {
    [attributeId: string]: {
      width?: number;
    };
  };
};

const getInitialColumnWidthPreferences = (layoutPreferences?: LayoutPreferences): ColumnWidthPreferences =>
  Object.fromEntries(
    Object.entries(layoutPreferences?.attributes ?? {}).flatMap(([key, { width }]) =>
      typeof width === 'number' ? [[key, width]] : [],
    ),
  );

const useInternalLayoutPreferences = ({
  layoutPreferences,
  defaultColumnOrder,
}: {
  layoutPreferences?: LayoutPreferences;
  defaultColumnOrder: Array<string>;
}) => {
  const getInitialState = () => ({
    internalAttributeColumnOrder: layoutPreferences?.order ?? defaultColumnOrder,
    internalColumnWidthPreferences: getInitialColumnWidthPreferences(layoutPreferences),
  });

  const [prevInitialState, setPrevInitialState] = useState(getInitialState);

  const [internalAttributeColumnOrder, setInternalAttributeColumnOrder] = useState<Array<string>>(
    () => prevInitialState.internalAttributeColumnOrder,
  );

  const [internalColumnWidthPreferences, setInternalColumnWidthPreferences] = useState<ColumnWidthPreferences>(
    () => prevInitialState.internalColumnWidthPreferences,
  );

  const nextInitialState = getInitialState();

  if (!isEqual(nextInitialState, prevInitialState)) {
    setPrevInitialState(nextInitialState);
    setInternalAttributeColumnOrder(nextInitialState.internalAttributeColumnOrder);
    setInternalColumnWidthPreferences(nextInitialState.internalColumnWidthPreferences);
  }

  return {
    internalAttributeColumnOrder,
    setInternalAttributeColumnOrder,
    internalColumnWidthPreferences,
    setInternalColumnWidthPreferences,
  };
};

export default useInternalLayoutPreferences;
