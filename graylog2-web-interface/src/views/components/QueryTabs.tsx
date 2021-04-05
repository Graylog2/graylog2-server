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
import { useEffect, useRef } from 'react';
import * as Immutable from 'immutable';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import throttle from 'lodash/throttle';

import { Col, Row, Nav, NavItem, NavDropdown, MenuItem } from 'components/graylog';
import { Icon } from 'components/common';
import QueryTitle from 'views/components/queries/QueryTitle';
import QueryTitleEditModal from 'views/components/queries/QueryTitleEditModal';
import Query, { QueryId } from 'views/logic/queries/Query';
import type { TitlesMap } from 'views/stores/TitleTypes';
import ViewState from 'views/logic/views/ViewState';

const StyledQueryNav = styled(Nav)(({ theme }) => css`
  &.nav.nav-tabs {
    border-bottom: 0;
    display: flex;
    white-space: nowrap;
    z-index: 2; // without it renders under widget management icons
    position: relative;

    > li {
      > a {
        color: ${theme.colors.variant.default};
        border: none;
        
        :hover,
        :active,
        :focus {
          transition: color 150ms ease-in-out;
          background: transparent;
          color: ${theme.colors.variant.primary};
        }
      }
    }

    > li.active {
      z-index: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      padding-bottom: 9px;

      > a {
        padding: 9px 15px 0;
        border: none;
        background-color: transparent;
        color: ${theme.colors.variant.darker.primary};

        :hover,
        :active,
        :focus {
          background: transparent;
          color: ${theme.colors.variant.darker.primary};
        }
      }
    }
    
    > li.query-tabs-more {
      cursor: pointer;
    }
  }
`);

type Props = {
  onRemove: (queryId: string) => Promise<void> | Promise<ViewState>,
  onSelect: (queryId: string) => Promise<Query> | Promise<string>,
  onTitleChange: (queryId: string, newTitle: string) => Promise<TitlesMap>,
  queries: Array<QueryId>,
  selectedQueryId: string,
  titles: Immutable.Map<string, string>,
};

const adjustTabs = () => {
  const dashboardTabs = document.querySelector('#dashboard-tabs') as HTMLElement;
  const allTabs = dashboardTabs.querySelectorAll('li');
  const tabItems = dashboardTabs.querySelectorAll('li:not(.dropdown):not(.query-tabs-new):not(.dropdown-menu li)');
  const moreItems = dashboardTabs.querySelectorAll('li.dropdown .dropdown-menu li');
  const moreBtn = dashboardTabs.querySelector('li.query-tabs-more') as HTMLElement;
  const newBtn = dashboardTabs.querySelector('li.query-tabs-new') as HTMLElement;

  let maxWidth = moreBtn.offsetWidth + newBtn.offsetWidth + 30; // magic number is PageContentLayout__Container padding
  const hiddenItems = [];
  const primaryWidth = dashboardTabs.offsetWidth;

  moreBtn.classList.remove('active');

  allTabs.forEach((tabItem) => {
    tabItem.classList.remove('hidden');
  });

  tabItems.forEach((tabItem: HTMLElement, idx) => {
    maxWidth += tabItem.offsetWidth;

    if (primaryWidth >= maxWidth) {
      tabItem.classList.remove('hidden');
      hiddenItems.splice(idx, 1);
    } else {
      tabItem.classList.add('hidden');
      hiddenItems.push(idx);
    }
  });

  moreItems.forEach((tabItem: HTMLElement, idx) => {
    if (!hiddenItems.includes(idx)) {
      tabItem.classList.add('hidden');
    }
  });

  if (!hiddenItems.length) {
    moreBtn.classList.add('hidden');
  }
};

const QueryTabs = ({ onRemove, onSelect, onTitleChange, queries, selectedQueryId, titles }:Props) => {
  const queryTitleEditModal = useRef<QueryTitleEditModal | undefined | null>();

  const openTitleEditModal = (activeQueryTitle: string) => {
    if (queryTitleEditModal) {
      queryTitleEditModal.current.open(activeQueryTitle);
    }
  };

  const queryTabs = (itemType: 'navItem' | 'menuItem') => queries.map((id, index) => {
    const title = titles.get(id, `Page#${index + 1}`);
    const tabTitle = (
      <QueryTitle active={id === selectedQueryId}
                  id={id}
                  onClose={() => onRemove(id)}
                  openEditModal={openTitleEditModal}
                  title={title} />
    );

    const output = {
      navItem: <NavItem eventKey={id} key={id} onClick={() => onSelect(id)}>{tabTitle}</NavItem>,
      menuItem: <MenuItem eventKey={id} key={id} onClick={() => onSelect(id)}>{tabTitle}</MenuItem>,
    };

    return output[itemType];
  });

  const newTab = (
    <NavItem key="new"
             eventKey="new"
             onClick={() => onSelect('new')}
             className="query-tabs-new">
      <Icon name="plus" />
    </NavItem>
  );

  const dropDownTabs = queryTabs('menuItem');

  useEffect(() => {
    adjustTabs();
    window.addEventListener('resize', throttle(adjustTabs, 250));
  });

  return (
    <Row style={{ marginBottom: 0 }}>
      <Col>
        <StyledQueryNav bsStyle="tabs" activeKey={selectedQueryId} id="dashboard-tabs">
          {queryTabs('navItem')}
          <NavDropdown eventKey="more"
                       title={<Icon name="ellipsis-h" />}
                       className="query-tabs-more"
                       id="query-tabs-more"
                       noCaret
                       pullRight>
            {dropDownTabs}
          </NavDropdown>
          {newTab}
        </StyledQueryNav>

        {/*
          The title edit modal can't be part of the QueryTitle component,
          due to the react bootstrap tabs keybindings.
          The input would always lose the focus when using the arrow keys.
        */}
        <QueryTitleEditModal onTitleChange={(newTitle: string) => onTitleChange(selectedQueryId, newTitle)}
                             ref={queryTitleEditModal} />
      </Col>
    </Row>
  );
};

QueryTabs.propTypes = {
  onRemove: PropTypes.func.isRequired,
  onSelect: PropTypes.func.isRequired,
  onTitleChange: PropTypes.func.isRequired,
  queries: PropTypes.object.isRequired,
  selectedQueryId: PropTypes.string.isRequired,
  titles: PropTypes.object.isRequired,
};

export default QueryTabs;
