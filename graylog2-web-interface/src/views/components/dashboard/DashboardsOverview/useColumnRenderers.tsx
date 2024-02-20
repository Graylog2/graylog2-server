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
import { useQueryClient } from '@tanstack/react-query';

import type { SearchParams } from 'stores/PaginationTypes';
import usePluginEntities from 'hooks/usePluginEntities';
import type { ColumnRenderers } from 'components/common/EntityDataTable';
import type View from 'views/logic/views/View';
import TitleCell from 'views/components/dashboard/DashboardsOverview/TitleCell';
import FavoriteIcon from 'views/components/FavoriteIcon';
import { createGRN } from 'logic/permissions/GRN';

export const useColumnRenderers = ({ searchParams }: { searchParams: SearchParams }) => {
  const queryClient = useQueryClient();
  const requirementsProvided = usePluginEntities('views.requires.provided');
  const customColumnRenderers: ColumnRenderers<View> = useMemo(
    () => ({
      attributes: {
        title: {
          renderCell: (_title: string, dashboard) => (
            <TitleCell dashboard={dashboard} requirementsProvided={requirementsProvided} />
          ),
        },
        favorite: {
          renderCell: (favorite: boolean, dashboard) => (
            <FavoriteIcon
              isFavorite={favorite}
              grn={createGRN('dashboard', dashboard.id)}
              onChange={(newValue) => {
                queryClient.setQueriesData(
                  ['dashboards', 'overview', searchParams],
                  (cur: { list: Readonly<Array<View>>; pagination: { total: number } }) => ({
                    ...cur,
                    list: cur.list.map((view) => {
                      if (view.id === dashboard.id) {
                        return view.toBuilder().favorite(newValue).build();
                      }

                      return view;
                    }),
                  }),
                );
              }}
            />
          ),
        },
      },
    }),
    [queryClient, requirementsProvided, searchParams],
  );

  return customColumnRenderers;
};

export default useColumnRenderers;
