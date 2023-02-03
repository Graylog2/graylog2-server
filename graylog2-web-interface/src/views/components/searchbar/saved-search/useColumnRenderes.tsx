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
import { useQueryClient } from '@tanstack/react-query';

import type { ColumnRenderers } from 'components/common/EntityDataTable';
import type View from 'views/logic/views/View';
import ViewLoaderContext from 'views/logic/ViewLoaderContext';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import FavoriteIcon from 'views/components/FavoriteIcon';
import type { SearchParams } from 'stores/PaginationTypes';

type Options = {
  enabled: boolean,
}

const onLoad = (
  onLoadSavedSearch: () => void,
  selectedSavedSearchId: string,
  loadFunc: (searchId: string) => void,
) => {
  if (!selectedSavedSearchId || !loadFunc) {
    return false;
  }

  loadFunc(selectedSavedSearchId);

  onLoadSavedSearch();

  return false;
};

const useColumnRenderers = (
  onLoadSavedSearch: () => void,
  searchParams: SearchParams,
): ColumnRenderers<View> => {
  const queryClient = useQueryClient();

  return ({
    title: {
      renderCell: (search) => (
        <ViewLoaderContext.Consumer key={search.id}>
          {(loaderFunc) => {
            const onClick = (e) => {
              e.preventDefault();
              onLoad(onLoadSavedSearch, search.id, loaderFunc);
            };

            return (
              <Link onClick={onClick}
                    to={Routes.getPluginRoute('SEARCH_VIEWID')(search.id)}>
                {search.title}
              </Link>
            );
          }}
        </ViewLoaderContext.Consumer>
      ),
    },
    favorite: {
      renderCell: (search) => (
        <FavoriteIcon isFavorite={search.favorite}
                      id={search.id}
                      onChange={(newValue) => {
                        queryClient.setQueriesData(['saved-searches', 'overview', searchParams], (cur: {
                          list: Array<View>,
                          pagination: { total: number }
                        }) => ({
                          ...cur,
                          list: cur.list.map((view) => {
                            if (view.id === search.id) {
                              return ({ ...view, favorite: newValue });
                            }

                            return view;
                          }),
                        }
                        ));
                      }} />
      ),
    },
  });
};

export default useColumnRenderers;
