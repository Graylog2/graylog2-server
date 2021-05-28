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
import { useEffect, useMemo, useState, ReactNode } from 'react';
import styled, { css } from 'styled-components';
import ImmutablePropTypes from 'react-immutable-proptypes';

import { QueryId } from 'views/logic/queries/Query';
import QueryTitleEditModal from 'views/components/queries/QueryTitleEditModal';
import { Nav, NavItem, MenuItem } from 'components/graylog';
import { ModifiedNavDropdown as NavDropdown } from 'components/graylog/NavDropdown';
import { Icon } from 'components/common';
import QueryTitle from 'views/components/queries/QueryTitle';

import { QueryTabsProps } from './QueryTabs';

interface Props extends QueryTabsProps {
  maxWidth: number,
  queryTitleEditModal: React.RefObject<QueryTitleEditModal>
}

interface TabsTypes {
  navItems: Array<ReactNode>,
  menuItems: Array<ReactNode>,
  lockedItems: Array<ReactNode>
}

const CLASS_HIDDEN = 'hidden';
const CLASS_LOCKED = 'locked';
const CLASS_ACTIVE = 'active';
const NAV_PADDING = 15;

const StyledQueryNav = styled(Nav)(({ theme }) => css`
  &.nav.nav-tabs {
    border-bottom: 0;
    display: flex;
    white-space: nowrap;
    z-index: 2; /* without it renders under widget management icons */
    position: relative;
    margin-bottom: -1px;
    padding-left: ${NAV_PADDING}px;

    > li {
      > a {
        color: ${theme.colors.variant.dark.default};
        border: none;
        
        :hover,
        :active,
        :focus {
          transition: color 150ms ease-in-out;
          background: transparent;
          color: ${theme.colors.variant.dark.primary};
        }
      }
    }

    > li.active {
      z-index: 1;
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

        :hover,
        :active,
        :focus {
          border: 1px solid ${theme.colors.variant.lighter.default};
          border-bottom: none;
          color: ${theme.colors.variant.darkest.primary};
        }
      }
    }

    > li.query-tabs-more,
    > li.query-tabs-more a {
      cursor: pointer;
    }
  }
`);

const adjustTabsVisibility = (maxWidth, lockedTab, setLockedTab) => {
  const dashboardTabs = document.querySelector('#dashboard-tabs') as HTMLElement;
  const tabItems = dashboardTabs.querySelectorAll(':scope > li:not(.dropdown):not(.query-tabs-new)') as NodeListOf<HTMLElement>;
  const moreItems = dashboardTabs.querySelectorAll('li.dropdown .dropdown-menu li') as NodeListOf<HTMLElement>;
  const moreBtn = dashboardTabs.querySelector('li.query-tabs-more') as HTMLElement;
  const newBtn = dashboardTabs.querySelector('li.query-tabs-new') as HTMLElement;
  const hiddenItems = [];

  let buttonsWidth = moreBtn.offsetWidth + newBtn.offsetWidth + NAV_PADDING;
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
      const { tabId } = tabItem.querySelector('a').dataset;

      setLockedTab(tabId);
    }
  });

  if (hiddenItems.length && moreBtn.classList.contains(CLASS_HIDDEN)) {
    moreBtn.classList.remove(CLASS_HIDDEN);
  } else if (!hiddenItems.length && !moreBtn.classList.contains(CLASS_HIDDEN)) {
    moreBtn.classList.add(CLASS_HIDDEN);
  }
};

const AdaptableQueryTabs = ({ maxWidth, queries, titles, selectedQueryId, onRemove, onSelect, queryTitleEditModal, onTitleChange }:Props) => {
  const [openedMore, setOpenedMore] = useState<boolean>(false);
  const [lockedTab, setLockedTab] = useState<QueryId>();

  const currentTabs = useMemo((): TabsTypes => {
    const navItems = [];
    const menuItems = [];
    const lockedItems = [];

    queries.forEach((id) => {
      const openTitleEditModal = (activeQueryTitle: string) => {
        if (queryTitleEditModal) {
          queryTitleEditModal.current.open(activeQueryTitle);
        }
      };

      if (!titles.has(id) && lockedTab !== id && selectedQueryId === id) {
        onTitleChange(id, `Page#${queries.size}`).then(() => setLockedTab(id));
      }

      const title = titles.get(id, `Page#${queries.size}`);
      const tabTitle = (
        <QueryTitle active={id === selectedQueryId}
                    id={id}
                    onClose={() => onRemove(id)}
                    openEditModal={openTitleEditModal}
                    title={title} />
      );

      navItems.push(lockedTab === id ? null : (
        <NavItem eventKey={id}
                 key={id}
                 data-tab-id={id}
                 onClick={() => {
                   setLockedTab(undefined);
                   onSelect(id);
                 }}>
          {tabTitle}
        </NavItem>
      ));

      menuItems.push(lockedTab === id ? null : (
        <MenuItem eventKey={id}
                  key={id}
                  data-tab-id={id}
                  onClick={() => {
                    setLockedTab(id);
                    onSelect(id);
                  }}>{tabTitle}
        </MenuItem>
      ));

      lockedItems.push(lockedTab !== id ? null : (
        <NavItem eventKey={id}
                 key={id}
                 data-tab-id={id}
                 onClick={() => onSelect(id)}
                 className={CLASS_LOCKED}>
          {tabTitle}
        </NavItem>
      ));
    });

    return { navItems, menuItems, lockedItems };
  }, [lockedTab, onRemove, onSelect, onTitleChange, queries, queryTitleEditModal, selectedQueryId, titles]);

  useEffect(() => {
    adjustTabsVisibility(maxWidth, lockedTab, setLockedTab);
  }, [maxWidth, lockedTab, selectedQueryId]);

  return (
    <StyledQueryNav bsStyle="tabs" activeKey={selectedQueryId} id="dashboard-tabs">
      {currentTabs.navItems}

      <NavDropdown eventKey="more"
                   title={<Icon name="ellipsis-h" />}
                   className="query-tabs-more"
                   id="query-tabs-more"
                   aria-label="More Dashboard Tabs"
                   noCaret
                   pullRight
                   active={openedMore}
                   open={openedMore}
                   onToggle={(isOpened) => setOpenedMore(isOpened)}>
        {currentTabs.menuItems}
      </NavDropdown>

      {currentTabs.lockedItems}

      <NavItem key="new"
               eventKey="new"
               title="Create New Tab"
               onClick={() => onSelect('new')}
               className="query-tabs-new">
        <Icon name="plus" />
      </NavItem>
    </StyledQueryNav>
  );
};

AdaptableQueryTabs.propTypes = {
  maxWidth: PropTypes.number.isRequired,
  queries: ImmutablePropTypes.orderedSetOf(PropTypes.string).isRequired,
  titles: PropTypes.object.isRequired,
  selectedQueryId: PropTypes.string.isRequired,
  onRemove: PropTypes.func.isRequired,
  onSelect: PropTypes.func.isRequired,
  queryTitleEditModal: PropTypes.oneOfType([
    PropTypes.func,
    PropTypes.shape({ current: PropTypes.object }),
  ]).isRequired,
  onTitleChange: PropTypes.func.isRequired,
};

export default AdaptableQueryTabs;
