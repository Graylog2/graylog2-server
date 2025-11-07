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

const columnPreferences = (userColumnPreferneces: ColumnPreferences, defaultAttributes: Array<string>) => {
  const result = defaultAttributes.reduce<ColumnPreferences>(
    (acc, attr) => ({
      ...acc,
      [attr]: userColumnPreferneces?.[attr] ?? { status: 'show' }, // if there is no user preference for a default field, show it
    }),
    {},
  );

  if (userColumnPreferneces) {
    for (const [attr, value] of Object.entries(userColumnPreferneces)) {
      if (!(attr in result)) {
        result[attr] = value;
      }
    }
  }

  return result;
};

const useTableLayout = ({ entityTableId, defaultSort, defaultPageSize, defaultDisplayedAttributes }: DefaultLayout) => {
  const { data: userLayoutPreferences = {}, isInitialLoading } = useUserLayoutPreferences(entityTableId);

  const test = columnPreferences(userLayoutPreferences?.attributes, defaultDisplayedAttributes);

  return useMemo(
    () => ({
      layoutConfig: {
        pageSize: userLayoutPreferences.perPage ?? defaultPageSize,
        sort: userLayoutPreferences.sort ?? defaultSort,
        columnPreferences: test,
      },
      isInitialLoading,
    }),
    [defaultPageSize, defaultSort, isInitialLoading, test, userLayoutPreferences.perPage, userLayoutPreferences.sort],
  );
};

export default useTableLayout;
