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

import usePluginEntities from 'hooks/usePluginEntities';
import type { ColumnRenderers } from 'components/common/EntityDataTable';
import type View from 'views/logic/views/View';
import TitleCell from 'views/components/dashboard/DashboardsOverview/TitleCell';
import FavoriteIcon from 'views/components/FavoriteIcon';
import { createGRN } from 'logic/permissions/GRN';
import { useTableFetchContext } from 'components/common/PaginatedEntityTable';

const DashboardFavoriteItem = ({ favorite, dashboardId }: { favorite: boolean, dashboardId: string }) => {
  const queryClient = useQueryClient();
  const { searchParams } = useTableFetchContext();

  return (
    <FavoriteIcon isFavorite={favorite}
                  grn={createGRN('dashboard', dashboardId)}
                  onChange={(newValue) => {
                    queryClient.setQueriesData(['dashboards', 'overview', searchParams], (cur: {
                      list: Readonly<Array<View>>,
                      pagination: { total: number }
                    }) => ({
                      ...cur,
                      list: cur.list.map((view) => {
                        if (view.id === dashboardId) {
                          return view.toBuilder().favorite(newValue).build();
                        }

                        return view;
                      }),
                    }
                    ));
                  }} />
  );
};

export const useColumnRenderers = () => {
  const requirementsProvided = usePluginEntities('views.requires.provided');
  const customColumnRenderers: ColumnRenderers<View> = useMemo(() => ({
    attributes: {
      title: {
        renderCell: (_title: string, dashboard) => <TitleCell dashboard={dashboard} requirementsProvided={requirementsProvided} />,
      },
      favorite: {
        renderCell: (favorite: boolean, dashboard) => (
          <DashboardFavoriteItem dashboardId={dashboard.id} favorite={favorite} />
        ),
      },
    },
  }), [requirementsProvided]);

  return customColumnRenderers;
};

export default useColumnRenderers;
