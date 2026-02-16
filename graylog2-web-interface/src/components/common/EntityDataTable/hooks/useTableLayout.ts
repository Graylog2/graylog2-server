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
import { useMemo } from 'react';

import type { DefaultLayout, ColumnPreferences } from 'components/common/EntityDataTable/types';

import useUserLayoutPreferences from './useUserLayoutPreferences';

export type LayoutConfig = {
  attributes: ColumnPreferences;
  order: Array<string>;
  pageSize: number;
  sort: DefaultLayout['defaultSort'];
};

const useTableLayout = ({
  entityTableId,
  defaultSort,
  defaultPageSize,
}: DefaultLayout): {
  isInitialLoading: boolean;
  layoutConfig: LayoutConfig;
} => {
  const { data: userLayoutPreferences = {}, isInitialLoading } = useUserLayoutPreferences(entityTableId);

  return useMemo(
    () => ({
      layoutConfig: {
        attributes: userLayoutPreferences?.attributes,
        order: userLayoutPreferences.order,
        pageSize: userLayoutPreferences.perPage ?? defaultPageSize,
        sort: userLayoutPreferences.sort ?? defaultSort,
      },
      isInitialLoading,
    }),
    [
      defaultPageSize,
      defaultSort,
      isInitialLoading,
      userLayoutPreferences?.attributes,
      userLayoutPreferences.order,
      userLayoutPreferences.perPage,
      userLayoutPreferences.sort,
    ],
  );
};

export default useTableLayout;
