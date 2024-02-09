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
import PropTypes from 'prop-types';
import type { ReactNode } from 'react';
import { useEffect, useMemo, useState, useCallback, useRef } from 'react';
import styled, { css } from 'styled-components';
import ImmutablePropTypes from 'react-immutable-proptypes';
import { OrderedSet } from 'immutable';

import UserNotification from 'util/UserNotification';
import type { QueryId } from 'views/logic/queries/Query';
import type QueryTitleEditModal from 'views/components/queries/QueryTitleEditModal';
import { Nav, NavItem, DropdownButton, MenuItem } from 'components/bootstrap';
import { Icon, IconButton } from 'components/common';
import QueryTitle from 'views/components/queries/QueryTitle';
import AdaptableQueryTabsConfiguration from 'views/components/AdaptableQueryTabsConfiguration';
import CopyToDashboardForm from 'views/components/widgets/CopyToDashboardForm';
import View from 'views/logic/views/View';
import type { SearchJson } from 'views/logic/search/Search';
import Search from 'views/logic/search/Search';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import CopyPageToDashboard from 'views/logic/views/CopyPageToDashboard';
import { loadAsDashboard, loadDashboard } from 'views/logic/views/Actions';
import createSearch from 'views/logic/slices/createSearch';
import type { AppDispatch } from 'stores/useAppDispatch';
import useAppDispatch from 'stores/useAppDispatch';
import type { GetState } from 'views/types';
import { selectView, selectActiveQuery } from 'views/logic/slices/viewSelectors';
import fetchSearch from 'views/logic/views/fetchSearch';
import type { HistoryFunction } from 'routing/useHistory';
import useHistory from 'routing/useHistory';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useCurrentQueryId from 'views/logic/queries/useCurrentQueryId';
import useView from 'views/hooks/useView';
import useIsNew from 'views/hooks/useIsNew';

import type { QueryTabsProps } from './QueryTabs';

interface Props extends QueryTabsProps {
  maxWidth: number,

  queryTitleEditModal: React.RefObject<QueryTitleEditModal>
}

interface TabsTypes {
  navItems: OrderedSet<ReactNode>,

  menuItems: OrderedSet<ReactNode>,

  lockedItems: OrderedSet<ReactNode>,

  queriesList: OrderedSet<{ id: string, title: string }>,
}

const CLASS_HIDDEN = 'hidden';
const CLASS_LOCKED = 'locked';
const CLASS_ACTIVE = 'active';
const NAV_PADDING = 15;
const TAB_MENU_ITEM_CLASS = 'tab-menu-item';
const MORE_TABS_BUTTON_CLASS = 'query-tabs-more';
const MORE_TABS_LI_CLASS = 'query-tabs-more-li';
const NEW_TAB_BUTTON_CLASS = 'query-tab-create';

const Container = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
`;

const StyledQueryNav = styled(Nav)(({ theme }) => css`
  &.nav.nav-tabs {
    border-bottom: 0;
    display: flex;
    white-space: nowrap;
    position: relative;
    padding-left: ${NAV_PADDING}px;

    > li {
      > a {
        color: ${theme.colors.variant.dark.default};
        border: none;

        &:hover,
        &:active,
        &:focus {
          transition: color 150ms ease-in-out;
          background: transparent;
          color: ${theme.colors.variant.dark.primary};
        }
      }
    }

    > li.active {
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-bottom: -3px;

      > a {
        padding: 9px 15px;
        border: 1px solid ${theme.colors.variant.lighter.default};
        border-bottom: none;
        background-color: ${theme.colors.global.contentBackground};
        color: ${theme.colors.variant.darkest.primary};

        &:hover,
        &:active,
        &:focus {
          border: 1px solid ${theme.colors.variant.lighter.default};
          border-bottom: none;
          color: ${theme.colors.variant.darkest.primary};
        }
      }
    }

    > li.${MORE_TABS_BUTTON_CLASS},
    > li.${MORE_TABS_BUTTON_CLASS} a {
      cursor: pointer;
    }
  }
`);

const QueryTab = styled(NavItem)`
  &&&&.active > a {
    padding: 6px 15px 9px;
  }
`;

const MoreTabsLi = ({ menuItems }: { menuItems: OrderedSet<React.ReactNode> }) => (
  <li className={MORE_TABS_LI_CLASS}>
    <DropdownButton title={<Icon name="ellipsis-h" />}
                    className={MORE_TABS_BUTTON_CLASS}
                    id="query-tabs-more"
                    aria-label="More Dashboard Pages"
                    noCaret
                    bsStyle="link"
                    keepMounted
                    pullRight>
      {menuItems.toArray()}
    </DropdownButton>
  </li>
);

const adjustTabsVisibility = (
  maxWidth: number,
  lockedTab: string | undefined,
  setLockedTab: React.Dispatch<React.SetStateAction<string>>,
  queriesConfigBtn: HTMLElement | null,
) => {
  const dashboardTabs = document.querySelector('#dashboard-tabs') as HTMLElement;
  const tabItems = dashboardTabs.querySelectorAll(`:scope > li:not(.${MORE_TABS_LI_CLASS}):not(.${NEW_TAB_BUTTON_CLASS})`) as NodeListOf<HTMLElement>;
  const moreItems = dashboardTabs.querySelectorAll(`li.${MORE_TABS_LI_CLASS} [role="menu"] a.${TAB_MENU_ITEM_CLASS}`) as NodeListOf<HTMLElement>;
  const moreBtn = dashboardTabs.querySelector(`.${MORE_TABS_BUTTON_CLASS}`) as HTMLElement;
  const newBtn = dashboardTabs.querySelector(`.${NEW_TAB_BUTTON_CLASS}`) as HTMLElement;
  const hiddenItems = [];

  let buttonsWidth = moreBtn.offsetWidth + newBtn.offsetWidth + (queriesConfigBtn?.offsetWidth ?? 0) + NAV_PADDING;
  let topTabsWidth = 0;

  tabItems.forEach((tabItem) => {
    tabItem.classList.remove(CLASS_HIDDEN);
    tabItem.setAttribute('aria-hidden', 'false');

    if (lockedTab) {
      const anchor = tabItem.querySelector('a');
      const { tabId } = anchor.dataset;

      if (tabId === lockedTab) {
        buttonsWidth += tabItem.offsetWidth;
      }
    }
  });

  tabItems.forEach((tabItem, idx) => {
    if (!tabItem.classList.contains(CLASS_LOCKED)) {
      topTabsWidth += tabItem.offsetWidth;

      if (maxWidth >= topTabsWidth + buttonsWidth) {
        hiddenItems.splice(idx, 1);
      } else {
        tabItem.classList.add(CLASS_HIDDEN);
        tabItem.setAttribute('aria-hidden', 'true');

        hiddenItems.push(idx);
      }
    }
  });

  moreItems.forEach((tabItem: HTMLElement, idx) => {
    tabItem.classList.remove(CLASS_HIDDEN);
    tabItem.setAttribute('aria-hidden', 'false');

    if (!hiddenItems.includes(idx)) {
      tabItem.classList.add(CLASS_HIDDEN);
      tabItem.setAttribute('aria-hidden', 'true');
    } else if (tabItem.classList.contains(CLASS_ACTIVE)) {
      const { tabId } = tabItem.dataset;
      setLockedTab(tabId);
    }
  });

  if (hiddenItems.length && moreBtn.classList.contains(CLASS_HIDDEN)) {
    moreBtn.classList.remove(CLASS_HIDDEN);
  } else if (!hiddenItems.length && !moreBtn.classList.contains(CLASS_HIDDEN)) {
    moreBtn.classList.add(CLASS_HIDDEN);
  }
};

const _updateDashboardWithNewSearch = (dashboard: View, newSearch: Search) => {
  const newDashboard = dashboard.toBuilder().search(newSearch).build();

  return ViewManagementActions.update(newDashboard);
};

const addPageToDashboard = (targetDashboard: View, activeView: View, queryId: string) => async (searchJson: SearchJson) => {
  const search = Search.fromJSON(searchJson);
  const newDashboard = CopyPageToDashboard(queryId, activeView, targetDashboard.toBuilder().search(search).build());

  if (!newDashboard || !newDashboard.search) {
    throw Error('Copying the dashboard page failed.');
  }

  const newQueryId = newDashboard.search.queries.last().id;

  const newSearch = await createSearch(newDashboard.search);

  await _updateDashboardWithNewSearch(newDashboard, newSearch);

  return [newDashboard, newQueryId] as const;
};

const _onCopyToDashboard = (selectedDashboardId: string | undefined | null) => async (_dispatch: AppDispatch, getState: GetState) => {
  const view = selectView(getState());
  const queryId = selectActiveQuery(getState());

  const dashboardJson = await ViewManagementActions.get(selectedDashboardId);
  const targetDashboard = View.fromJSON(dashboardJson);

  return fetchSearch(dashboardJson.search_id)
    .then(addPageToDashboard(targetDashboard, view, queryId));
};

const _onCreateNewDashboard = async (view: View, queryId: string, history: HistoryFunction) => {
  const newDashboard = CopyPageToDashboard(queryId, view, View
    .create()
    .toBuilder()
    .state({})
    .type(View.Type.Dashboard)
    .search(Search.create().toBuilder().queries([]).build())
    .build());

  loadAsDashboard(history, newDashboard);
};

const AdaptableQueryTabs = ({
  maxWidth,
  queries,
  titles,
  onRemove,
  onSelect,
  queryTitleEditModal,
  dashboardId,
}: Props) => {
  const view = useView();
  const isNew = useIsNew();
  const activeQueryId = useCurrentQueryId();
  const [lockedTab, setLockedTab] = useState<QueryId>();
  const [showConfigurationModal, setShowConfigurationModal] = useState<boolean>(false);
  const [showCopyToDashboardModal, setShowCopyToDashboardModal] = useState<boolean>(false);
  const dispatch = useAppDispatch();
  const history = useHistory();
  const sendTelemetry = useSendTelemetry();
  const queriesConfigBtn = useRef(null);

  const toggleCopyToDashboardModal = useCallback(() => {
    setShowCopyToDashboardModal((cur) => !cur);
  }, []);

  const onCopyToDashboard = useCallback((selectedDashboardId: string) => dispatch(_onCopyToDashboard(selectedDashboardId))
    .then(([newDashboard, newQueryId]) => loadDashboard(history, newDashboard.id, newQueryId))
    .catch((error) => {
      UserNotification.error(`Copying dashboard page failed with error ${error}`);
    }), [dispatch, history]);

  const onCreateNewDashboard = () => _onCreateNewDashboard(view, activeQueryId, history);

  const openTitleEditModal = useCallback((activeQueryTitle: string) => {
    if (queryTitleEditModal) {
      queryTitleEditModal.current.open(activeQueryTitle);
    }
  }, [queryTitleEditModal]);

  const currentTabs = useMemo((): TabsTypes => {
    let navItems = OrderedSet<React.ReactNode>();
    let menuItems = OrderedSet<React.ReactNode>();
    let lockedItems = OrderedSet<React.ReactNode>();
    let queriesList = OrderedSet<{ id: string, title: string }>();

    queries.keySeq().forEach((id, idx) => {
      const title = titles.get(id, `Page#${idx + 1}`);
      const tabTitle = (
        <QueryTitle active={id === activeQueryId}
                    id={id}
                    onRemove={() => onRemove(id)}
                    openEditModal={openTitleEditModal}
                    openCopyToDashboardModal={toggleCopyToDashboardModal}
                    allowsClosing={queries.size > 1}
                    title={title} />
      );

      navItems = navItems.add(lockedTab === id ? null : (
        <QueryTab eventKey={id}
                  key={id}
                  data-tab-id={id}
                  onClick={() => {
                    setLockedTab(undefined);
                    onSelect(id);
                  }}>
          {tabTitle}
        </QueryTab>
      ));

      menuItems = menuItems.add(lockedTab === id ? null : (
        <MenuItem eventKey={id}
                  key={id}
                  component="a"
                  className={`${TAB_MENU_ITEM_CLASS} ${activeQueryId === id ? CLASS_ACTIVE : ''}`}
                  data-tab-id={id}
                  onClick={() => {
                    setLockedTab(id);
                    onSelect(id);
                  }}>
          {tabTitle}
        </MenuItem>
      ));

      lockedItems = lockedItems.add(lockedTab !== id ? null : (
        <QueryTab eventKey={id}
                  key={id}
                  data-tab-id={id}
                  onClick={() => onSelect(id)}
                  className={CLASS_LOCKED}>
          {tabTitle}
        </QueryTab>
      ));

      queriesList = queriesList.add({ id, title });
    });

    return { navItems, menuItems, lockedItems, queriesList };
  }, [queries, titles, activeQueryId, openTitleEditModal, toggleCopyToDashboardModal, lockedTab, onRemove, onSelect]);

  useEffect(() => {
    adjustTabsVisibility(maxWidth, lockedTab, setLockedTab, queriesConfigBtn.current);
  }, [maxWidth, lockedTab, activeQueryId]);

  return (
    <Container>
      <StyledQueryNav bsStyle="tabs" activeKey={activeQueryId} id="dashboard-tabs">
        {currentTabs.navItems.toArray()}

        <MoreTabsLi menuItems={currentTabs.menuItems} />

        {currentTabs.lockedItems.toArray()}

        <QueryTab key="new"
                  eventKey="new"
                  title="Create New Page"
                  onClick={() => {
                    sendTelemetry(TELEMETRY_EVENT_TYPE.DASHBOARD_ACTION.DASHBOARD_CREATE_PAGE, {
                      app_pathname: 'dashboard',
                      app_section: 'dashboard',
                      app_action_value: 'dashboard-create-page-button',
                    });

                    onSelect('new');
                  }}
                  className={NEW_TAB_BUTTON_CLASS}>
          <Icon name="plus" />
        </QueryTab>
      </StyledQueryNav>
      <IconButton title="Open pages configuration"
                  name="cog"
                  ref={queriesConfigBtn}
                  className="query-config-btn"
                  onClick={() => {
                    sendTelemetry(TELEMETRY_EVENT_TYPE.DASHBOARD_ACTION.DASHBOARD_PAGE_CONFIGURATION, {
                      app_pathname: 'dashboard',
                      app_section: 'dashboard',
                      app_action_value: 'dashboard-page-configuration-button',
                    });

                    setShowConfigurationModal(true);
                  }} />
      {showConfigurationModal && (
        <AdaptableQueryTabsConfiguration show={showConfigurationModal}
                                         setShow={setShowConfigurationModal}
                                         dashboardId={dashboardId}
                                         queriesList={currentTabs.queriesList}
                                         activeQueryId={activeQueryId} />
      )}
      {showCopyToDashboardModal && (
        <CopyToDashboardForm onCopyToDashboard={(selectedDashboardId) => onCopyToDashboard(selectedDashboardId)}
                             onCreateNewDashboard={isNew ? undefined : onCreateNewDashboard}
                             onCancel={toggleCopyToDashboardModal}
                             activeDashboardId={dashboardId}
                             submitButtonText="Copy page"
                             submitLoadingText="Copying page..." />
      )}
    </Container>
  );
};

AdaptableQueryTabs.propTypes = {
  maxWidth: PropTypes.number.isRequired,
  queries: ImmutablePropTypes.orderedSetOf(PropTypes.string).isRequired,
  titles: PropTypes.object.isRequired,
  onRemove: PropTypes.func.isRequired,
  onSelect: PropTypes.func.isRequired,
  queryTitleEditModal: PropTypes.oneOfType([
    PropTypes.func,
    PropTypes.shape({ current: PropTypes.object }),
  ]).isRequired,
};

export default AdaptableQueryTabs;
