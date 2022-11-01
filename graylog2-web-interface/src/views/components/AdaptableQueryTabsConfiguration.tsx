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
import styled from 'styled-components';

import BootstrapModalConfirm from 'components/bootstrap/BootstrapModalConfirm';
import { QueriesActions } from 'views/actions/QueriesActions';
import { SortableList, IconButton } from 'components/common';
import type { ListItemType } from 'components/common/SortableList/ListItem';
import { ViewStatesActions } from 'views/stores/ViewStatesStore';
import type { TitleType } from 'views/stores/TitleTypes';
import TitleTypes from 'views/stores/TitleTypes';
import EditableTitle from 'views/components/common/EditableTitle';

const ListItemContainer = styled.div`
  display: flex;
  justify-content: space-between;
  flex: 1;
`;

const ListItem = ({
  item: { id, title },
  onRemove,
  onUpdateTitle,
}: {
  item: { id: string, title?: string },
  onUpdateTitle: (id: string, title: string) => void,
  onRemove: (id: string) => void,
}) => {
  return (
    <ListItemContainer>
      <EditableTitle key={title} disabled={!onUpdateTitle} value={title} onChange={(newTitle) => onUpdateTitle(id, newTitle)} />
      <div>
        {/* <IconButton title={`Edit title for page ${title}`} name="edit" onClick={() => {}} /> */}
        <IconButton title={`Remove page ${title}`} name="trash" onClick={() => onRemove(id)} />
      </div>
    </ListItemContainer>
  );
};

type Props = {
  show: boolean,
  setShow: Dispatch<SetStateAction<boolean>>,
  queriesList: OrderedSet<ListItemType>
}

const AdaptableQueryTabsConfiguration = ({ show, setShow, queriesList }: Props) => {
  const [orderedQueriesList, setOrderedQueriesList] = useState<OrderedSet<ListItemType>>(queriesList);
  const onConfirmPagesConfiguration = useCallback(() => {
    QueriesActions.setOrder(OrderedSet(orderedQueriesList.map(({ id }) => id))).then(() => {
      const newTitles = orderedQueriesList.map(({ id, title }) => {
        const titleMap = Map<string, string>({ title });
        const titlesMap = Map<TitleType, Map<string, string>>({ [TitleTypes.Tab]: titleMap });

        return ({ queryId: id, titlesMap });
      });

      ViewStatesActions.patchQueriesTitle(Set(newTitles));
      setShow(false);
    });
  }, [orderedQueriesList, setShow]);
  const onPagesConfigurationModalClose = useCallback(() => setShow(false), [setShow]);
  const updatePageSorting = useCallback((order: Array<ListItemType>) => {
    setOrderedQueriesList(OrderedSet(order));
  }, [setOrderedQueriesList]);

  const updatePageTitle = (id: string, title: string) => {
    setOrderedQueriesList((currentQueries) => (
      OrderedSet(currentQueries.map((query) => {
        if (query.id === id) {
          return { id, title };
        }

        return query;
      }))),
    );
  };

  const removePage = (id: string) => {
    setOrderedQueriesList((currentQueries) => (
      OrderedSet(currentQueries.filter((query) => query.id !== id))),
    );
  };

  return (
    <BootstrapModalConfirm showModal={show}
                           title="Update Dashboard Pages Configuration"
                           onConfirm={onConfirmPagesConfiguration}
                           onModalClose={onPagesConfigurationModalClose}
                           confirmButtonText="Update configuration">
      <>
        <h3>Order</h3>
        <p>
          Use drag and drop to change the execution order of the dashboard pages.
          Click on a dashboard title to change it.
        </p>
        <SortableList items={orderedQueriesList.toArray()}
                      onMoveItem={updatePageSorting}
                      displayOverlayInPortal
                      customContentRender={({ item }) => <ListItem item={item} onUpdateTitle={updatePageTitle} onRemove={removePage} />} />
      </>
    </BootstrapModalConfirm>
  );
};

export default AdaptableQueryTabsConfiguration;
