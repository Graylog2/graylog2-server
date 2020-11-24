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
import * as Immutable from 'immutable';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

import { Tab, Tabs, Col, Row } from 'components/graylog';
import QueryTitle from 'views/components/queries/QueryTitle';
import QueryTitleEditModal from 'views/components/queries/QueryTitleEditModal';
import Query, { QueryId } from 'views/logic/queries/Query';
import type { TitlesMap } from 'views/stores/TitleTypes';
import ViewState from 'views/logic/views/ViewState';

const StyledQueryTabs = styled(Tabs)(({ theme }) => css`
  .tab-pane {
    display: none;
  }

  > .nav-tabs {
    border-bottom: 0;

    > li {
      > a {
        border-color: ${theme.colors.gray[80]};
      }
    }

    > li.active {
      z-index: 1;

      > a {
        border-bottom-color: transparent;
      }
    }
  }
`);

type Props = {
  children: React.ReactNode,
  onRemove: (queryId: string) => Promise<void> | Promise<ViewState>,
  onSelect: (queryId: string) => Promise<Query> | Promise<string>,
  onTitleChange: (queryId: string, newTitle: string) => Promise<TitlesMap>,
  queries: Array<QueryId>,
  selectedQueryId: string,
  titles: Immutable.Map<string, string>,
};

class QueryTabs extends React.Component<Props> {
  private queryTitleEditModal: QueryTitleEditModal | undefined | null;

  static propTypes = {
    children: PropTypes.node,
    onRemove: PropTypes.func.isRequired,
    onSelect: PropTypes.func.isRequired,
    onTitleChange: PropTypes.func.isRequired,
    queries: PropTypes.object.isRequired,
    selectedQueryId: PropTypes.string.isRequired,
    titles: PropTypes.object.isRequired,
  }

  static defaultProps = {
    children: null,
  }

  openTitleEditModal = (activeQueryTitle: string) => {
    if (this.queryTitleEditModal) {
      this.queryTitleEditModal.open(activeQueryTitle);
    }
  }

  render() {
    const {
      children,
      onRemove,
      onSelect,
      onTitleChange,
      queries,
      selectedQueryId,
      titles,
    } = this.props;
    const queryTitles = titles;
    const queryTabs = queries.map((id, index) => {
      const title = queryTitles.get(id, `Page#${index + 1}`);
      const tabTitle = (
        <QueryTitle active={id === selectedQueryId}
                    id={id}
                    onClose={() => onRemove(id)}
                    openEditModal={this.openTitleEditModal}
                    title={title} />
      );

      return (
        <Tab eventKey={id}
             key={id}
             mountOnEnter
             title={tabTitle}
             unmountOnExit>
          {children}
        </Tab>
      );
    });
    const newTab = <Tab key="new" eventKey="new" title="+" />;
    const tabs = [queryTabs, newTab];

    return (
      <Row style={{ marginBottom: 0 }}>
        <Col>
          <StyledQueryTabs activeKey={selectedQueryId}
                           animation={false}
                           id="dashboard-pages"
                           onSelect={onSelect}>
            {tabs}
          </StyledQueryTabs>
          {/*
          The title edit modal can't be part of the QueryTitle component,
          due to the react bootstrap tabs keybindings.
          The input would always lose the focus when using the arrow keys.
        */}
          <QueryTitleEditModal onTitleChange={(newTitle: string) => onTitleChange(selectedQueryId, newTitle)}
                               ref={(queryTitleEditModal) => { this.queryTitleEditModal = queryTitleEditModal; }} />
        </Col>
      </Row>
    );
  }
}

export default QueryTabs;
