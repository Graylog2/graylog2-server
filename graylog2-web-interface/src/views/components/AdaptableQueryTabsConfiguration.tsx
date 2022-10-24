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
import type { Dispatch, SetStateAction } from 'react';
import { useCallback, useState } from 'react';
import { OrderedSet, Map, Set } from 'immutable';

import BootstrapModalConfirm from 'components/bootstrap/BootstrapModalConfirm';
import { QueriesActions } from 'views/actions/QueriesActions';
import { SortableList } from 'components/common';
import type { ListItemType } from 'components/common/SortableList/ListItem';
import { ViewStatesActions } from 'views/stores/ViewStatesStore';
import type { TitleType } from 'views/stores/TitleTypes';
import type { QueryId } from 'views/logic/queries/Query';
import TitleTypes from 'views/stores/TitleTypes';

type Props = {
  show: boolean,
  setShow: Dispatch<SetStateAction<boolean>>,
  queriesList: Array<ListItemType>
}

const AdaptableQueryTabsConfiguration = ({ show, setShow, queriesList }: Props) => {
  const [orderedQueriesList, setOrderedQueriesList] = useState<Array<ListItemType>>(queriesList);
  const onConfirmPagesConfiguration = useCallback(() => {
    QueriesActions.setOrder(OrderedSet(orderedQueriesList)).then(() => {
      const newTitles = orderedQueriesList.map(({ id: queryId, title }: {
        id: QueryId, title: string
      }) => {
        const titleMap = Map<string, string>({ title });
        const titlesMap = Map<TitleType, Map<string, string>>({ [TitleTypes.Tab]: titleMap });

        return ({ queryId, titlesMap });
      });

      ViewStatesActions.patchQueriesTitle(Set(newTitles));
      setShow(false);
    });
  }, [orderedQueriesList, setShow]);
  const onPagesConfigurationModalClose = useCallback(() => setShow(false), [setShow]);
  const updateTabSorting = useCallback((order) => {
    setOrderedQueriesList(order);
  }, [setOrderedQueriesList]);

  return (
    <BootstrapModalConfirm showModal={show}
                           title="Update Dashboard Pages Configuration"
                           onConfirm={onConfirmPagesConfiguration}
                           onModalClose={onPagesConfigurationModalClose}
                           confirmButtonText="Update configuration">
      <>
        <h3>Order</h3>
        <p>Use drag and drop to change the execution order of the dashboard pages.</p>
        <SortableList items={orderedQueriesList} onMoveItem={updateTabSorting} displayOverlayInPortal />
      </>
    </BootstrapModalConfirm>
  );
};

export default AdaptableQueryTabsConfiguration;
