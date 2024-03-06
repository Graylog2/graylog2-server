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
import * as Immutable from 'immutable';
import styled from 'styled-components';

import BootstrapModalConfirm from 'components/bootstrap/BootstrapModalConfirm';
import { SortableList, IconButton } from 'components/common';
import type { TitleType, TitlesMap } from 'views/stores/TitleTypes';
import TitleTypes from 'views/stores/TitleTypes';
import EditableTitle from 'views/components/common/EditableTitle';
import DashboardPageContext from 'views/components/contexts/DashboardPageContext';
import FindNewActiveQueryId from 'views/logic/views/FindNewActiveQuery';
import useAppDispatch from 'stores/useAppDispatch';
import { setQueriesOrder, mergeQueryTitles } from 'views/logic/slices/viewSlice';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import ConfirmDeletingDashboardPage from 'views/logic/views/ConfirmDeletingDashboardPage';
import useWidgetIds from 'views/components/useWidgetIds';

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
}) => (
  <ListItemContainer>
    <EditableTitle key={title}
                   disabled={!onUpdateTitle}
                   value={title}
                   onChange={(newTitle) => onUpdateTitle(id, newTitle)} />
    <div>
      <IconButton title={`Remove page ${title}`}
                  name="delete"
                  onClick={() => onRemove(id)}
                  disabled={disableDelete} />
    </div>
  </ListItemContainer>
);

type Props = {
  show: boolean,
  setShow: Dispatch<SetStateAction<boolean>>,
  queriesList: Immutable.OrderedSet<PageListItem>,
  activeQueryId: string,
  dashboardId: string,
}

const AdaptableQueryTabsConfiguration = ({ show, setShow, queriesList, activeQueryId, dashboardId }: Props) => {
  const { setDashboardPage } = useContext(DashboardPageContext);
  const widgetIds = useWidgetIds();
  const [nextQueriesList, setNextQueriesList] = useState<Immutable.OrderedSet<PageListItem>>(queriesList);
  const disablePageDelete = nextQueriesList.size <= 1;
  const dispatch = useAppDispatch();
  const sendTelemetry = useSendTelemetry();
  const onConfirmPagesConfiguration = useCallback(() => {
    const isActiveQueryDeleted = !nextQueriesList.find(({ id }) => id === activeQueryId);

    sendTelemetry(TELEMETRY_EVENT_TYPE.DASHBOARD_ACTION.DASHBOARD_PAGE_CONFIGURATION_UPDATED, {
      app_pathname: 'dashboard',
      app_section: 'dashboard',
      app_action_value: 'dashboard-page-configuration',
    });

    if (isActiveQueryDeleted) {
      const indexedQueryIds = queriesList.map(({ id }) => id).toList();
      const nextQueryIds = nextQueriesList.map(({ id }) => id).toArray();
      const removedQueryIds = indexedQueryIds.filter((queryId) => !nextQueryIds.includes(queryId)).toList();
      const newActiveQueryId = FindNewActiveQueryId(indexedQueryIds, activeQueryId, removedQueryIds);
      setDashboardPage(newActiveQueryId);
    }

    dispatch(setQueriesOrder(nextQueriesList.map(({ id }) => id).toOrderedSet()))
      .then(() => {
        const newTitles = nextQueriesList.map(({ id, title }) => {
          const titleMap = Immutable.Map<string, string>({ title });
          const titlesMap = Immutable.Map<TitleType, Immutable.Map<string, string>>({ [TitleTypes.Tab]: titleMap }) as TitlesMap;

          return ({ queryId: id, titlesMap });
        }).toArray();

        dispatch(mergeQueryTitles(newTitles));
        setShow(false);
      });
  }, [nextQueriesList, sendTelemetry, dispatch, activeQueryId, queriesList, setDashboardPage, setShow]);

  const onPagesConfigurationModalClose = useCallback(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.DASHBOARD_ACTION.DASHBOARD_PAGE_CONFIGURATION_CANCELED, {
      app_pathname: 'dashboard',
      app_section: 'dashboard',
      app_action_value: 'dashboard-page-configuration',
    });

    setShow(false);
  }, [sendTelemetry, setShow]);
  const updatePageSorting = useCallback((order: Array<PageListItem>) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.DASHBOARD_ACTION.DASHBOARD_PAGE_CONFIGURATION_SORTING_UPDATED, {
      app_pathname: 'dashboard',
      app_section: 'dashboard',
      app_action_value: 'dashboard-page-configuration-sorting',
    });

    setNextQueriesList(Immutable.OrderedSet(order));
  }, [sendTelemetry, setNextQueriesList]);

  const onUpdateTitle = useCallback((id: string, title: string) => {
    setNextQueriesList((currentQueries) => currentQueries
      .map((query) => (query.id === id ? { id, title } : query))
      .toOrderedSet());
  }, []);

  const removePage = useCallback(async (queryId: string) => {
    if (disablePageDelete) {
      return Promise.resolve();
    }

    if (await ConfirmDeletingDashboardPage(dashboardId, activeQueryId, widgetIds)) {
      sendTelemetry(TELEMETRY_EVENT_TYPE.DASHBOARD_ACTION.DASHBOARD_PAGE_CONFIGURATION_PAGE_REMOVED, {
        app_pathname: 'dashboard',
        app_section: 'dashboard',
        app_action_value: 'dashboard-page-configuration-remove-page',
      });

      setNextQueriesList((currentQueries) => currentQueries
        .filter((query) => query.id !== queryId).toOrderedSet());
    }

    return Promise.resolve();
  }, [activeQueryId, dashboardId, disablePageDelete, sendTelemetry, widgetIds]);

  // eslint-disable-next-line react/no-unused-prop-types
  const customListItemRender = useCallback(({ item }: { item: PageListItem }) => (
    <ListItem item={item}
              onUpdateTitle={onUpdateTitle}
              onRemove={removePage}
              disableDelete={disablePageDelete} />
  ), [disablePageDelete, removePage, onUpdateTitle]);

  return (
    <BootstrapModalConfirm showModal={show}
                           title="Update Dashboard Pages Configuration"
                           onConfirm={onConfirmPagesConfiguration}
                           onCancel={onPagesConfigurationModalClose}
                           confirmButtonText="Update configuration">
      <>
        <h3>Order</h3>
        <p>
          Use drag and drop to change the order of the dashboard pages.
          Double-click on a dashboard title to change it.
        </p>
        <SortableList<PageListItem> items={nextQueriesList.toArray()}
                                    onMoveItem={updatePageSorting}
                                    displayOverlayInPortal
                                    alignItemContent="center"
                                    customContentRender={customListItemRender} />
      </>
    </BootstrapModalConfirm>
  );
};

export default AdaptableQueryTabsConfiguration;
