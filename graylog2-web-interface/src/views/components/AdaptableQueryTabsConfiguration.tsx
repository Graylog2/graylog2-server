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
import { useCallback, useState, useContext } from 'react';
import { OrderedSet, Map, Set, List } from 'immutable';
import styled from 'styled-components';

import BootstrapModalConfirm from 'components/bootstrap/BootstrapModalConfirm';
import { QueriesActions } from 'views/actions/QueriesActions';
import { SortableList, IconButton } from 'components/common';
import { ViewStatesActions, ViewStatesStore } from 'views/stores/ViewStatesStore';
import type { TitleType } from 'views/stores/TitleTypes';
import TitleTypes from 'views/stores/TitleTypes';
import EditableTitle from 'views/components/common/EditableTitle';
import { useStore } from 'stores/connect';
import DashboardPageContext from 'views/components/contexts/DashboardPageContext';
import FindNewActiveQueryId from 'views/logic/views/FindNewActiveQuery';
import ConfirmDeletingDashboardPage from 'views/logic/views/ConfirmDeletingDashboardPage';

type PageListItem = {
  id: string,
  title: string
}

const ListItemContainer = styled.div`
  display: flex;
  justify-content: space-between;
  flex: 1;
  overflow: hidden;
`;

const ListItem = ({
  item: { id, title },
  onRemove,
  onUpdateTitle,
  disableDelete,
}: {
  item: PageListItem,
  onUpdateTitle: (id: string, title: string) => void,
  onRemove: (id: string) => void,
  disableDelete: boolean,
}) => {
  return (
    <ListItemContainer>
      <EditableTitle key={title} disabled={!onUpdateTitle} value={title} onChange={(newTitle) => onUpdateTitle(id, newTitle)} />
      <div>
        <IconButton title={`Remove page ${title}`} name="trash" onClick={() => onRemove(id)} disabled={disableDelete} />
      </div>
    </ListItemContainer>
  );
};

const useWidgetIds = () => useStore(ViewStatesStore, (states) => states.map((viewState) => viewState.widgets.map((widget) => widget.id).toList()).toMap());

type Props = {
  show: boolean,
  setShow: Dispatch<SetStateAction<boolean>>,
  queriesList: OrderedSet<PageListItem>,
  dashboardId: string,
  activeQueryId: string,
}

const AdaptableQueryTabsConfiguration = ({ show, setShow, queriesList, dashboardId, activeQueryId }: Props) => {
  const widgetIds = useWidgetIds();
  const { setDashboardPage } = useContext(DashboardPageContext);
  const [orderedQueriesList, setOrderedQueriesList] = useState<OrderedSet<PageListItem>>(queriesList);
  const disablePageDelete = orderedQueriesList.size <= 1;
  const onConfirmPagesConfiguration = useCallback(() => {
    const isActiveQueryDeleted = !orderedQueriesList.find(({ id }) => id === activeQueryId);

    if (isActiveQueryDeleted) {
      const indexedQueryIds = queriesList.map(({ id }) => id).toIndexedSeq();
      const newActiveQueryId = FindNewActiveQueryId(List(indexedQueryIds), activeQueryId);

      setDashboardPage(newActiveQueryId);
    }

    QueriesActions.setOrder(OrderedSet(orderedQueriesList.map(({ id }) => id))).then(() => {
      const newTitles = orderedQueriesList.map(({ id, title }) => {
        const titleMap = Map<string, string>({ title });
        const titlesMap = Map<TitleType, Map<string, string>>({ [TitleTypes.Tab]: titleMap });

        return ({ queryId: id, titlesMap });
      });

      ViewStatesActions.patchQueriesTitle(Set(newTitles));
      setShow(false);
    });
  }, [orderedQueriesList, queriesList, activeQueryId, setDashboardPage, setShow]);
  const onPagesConfigurationModalClose = useCallback(() => setShow(false), [setShow]);
  const updatePageSorting = useCallback((order: Array<PageListItem>) => {
    setOrderedQueriesList(OrderedSet(order));
  }, [setOrderedQueriesList]);

  const onUpdateTitle = useCallback((id: string, title: string) => {
    setOrderedQueriesList((currentQueries) => (
      OrderedSet(currentQueries.map((query) => {
        if (query.id === id) {
          return { id, title };
        }

        return query;
      }))),
    );
  }, []);

  const onRemovePage = useCallback(async (id: string) => {
    if (disablePageDelete) {
      return Promise.resolve();
    }

    if (await ConfirmDeletingDashboardPage(dashboardId, activeQueryId, widgetIds)) {
      setOrderedQueriesList((currentQueries) => (
        OrderedSet(currentQueries.filter((query) => query.id !== id))),
      );
    }

    return Promise.resolve();
  }, [activeQueryId, dashboardId, disablePageDelete, widgetIds]);

  // eslint-disable-next-line react/no-unused-prop-types
  const customListItemRender = useCallback(({ item }: { item: PageListItem }) => (
    <ListItem item={item}
              onUpdateTitle={onUpdateTitle}
              onRemove={onRemovePage}
              disableDelete={disablePageDelete} />
  ), [disablePageDelete, onRemovePage, onUpdateTitle]);

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
          Double-click on a dashboard title to change it.
        </p>
        <SortableList<PageListItem> items={orderedQueriesList.toArray()}
                                    onMoveItem={updatePageSorting}
                                    displayOverlayInPortal
                                    alignItemContent="center"
                                    customContentRender={customListItemRender} />
      </>
    </BootstrapModalConfirm>
  );
};

export default AdaptableQueryTabsConfiguration;
