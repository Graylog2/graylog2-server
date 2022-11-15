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
import React from 'react';
import PropTypes from 'prop-types';
import Qs from 'qs';
import styled, { css } from 'styled-components';
import moment from 'moment';

import { Grid, Col, Button, Row, ListGroup, ListGroupItem, Table, Label } from 'components/bootstrap';
import {
  ContentHeadRow,
  Spinner,
  Icon,
  DocumentTitle,
  EntityListItem,
  ControlledTableList,
  LinkToNode, PaginatedList,
} from 'components/common';
import { GettingStartedActions } from 'stores/gettingstarted/GettingStartedStore';

import PageContentLayout from '../layout/PageContentLayout';
import AutoFontSizer from '../../views/components/visualizations/number/AutoFontSizer';
import CustomHighlighting from '../../views/components/messagelist/CustomHighlighting';
import Value from '../../views/components/Value';
import fieldTypeFor from '../../views/logic/fieldtypes/FieldTypeFor';
import DecoratedValue from '../../views/components/messagelist/decoration/DecoratedValue';
import ElementDimensions from '../common/ElementDimensions';
import { FieldTypes } from '../../views/logic/fieldtypes/FieldType';
import PageHeader from '../common/PageHeader';
import SectionGrid from '../common/Section/SectionGrid';
import SectionComponent from '../common/Section/SectionComponent';
import { Link } from '../common/router';
import Routes from '../../routing/Routes';
import ControlledTableListItem from '../common/ControlledTableListItem';
import { relativeDifference } from '../../util/DateTime';

const NumberBox = styled(ElementDimensions)`
  height: 100%;
  width: 100%;
  padding-bottom: 10px;
`;
const StyledLabel = styled(Label)`
  cursor: default;
  width: 85px;
  display: block;
`;
const FlexContainer = styled.div`
  display: flex;
  align-items: stretch;
  flex-wrap: nowrap;
  gap: 45px;
`;

const StyledSummary = styled.small(({ theme }) => css`
  color: ${theme.colors.variant.light.primary}
`);

const StyledSectionComponent = styled(SectionComponent)`
  flex-grow: 1;
`;

const StyledListGroupItem = styled(ListGroupItem)`
  display: flex;
  gap: 16px;
`;

const ActionItemLink = styled(Link)(({ theme }) => css`
  color: ${theme.colors.variant.primary};
  &:hover {
    color: ${theme.colors.variant.darker.primary};
  }
`);

const typeLinkMap = {
  dashboard: { link: 'DASHBOARDS_VIEWID', icon: 'd' },
  search: { link: 'SEARCH_VIEWID', icon: 's' },
};

const EntityItem = ({ type, title, id }) => {
  return (
    <StyledListGroupItem>
      <StyledLabel bsStyle="info">{type}</StyledLabel>
      <Link to={Routes.pluginRoute(typeLinkMap[type].link)(id)}>{title}</Link>
    </StyledListGroupItem>
  );
};

const lastOpen = [
  {
    type: 'dashboard',
    title: 'DashboardTitle1',
    id: '1111',
    summary: 'summary',
    description: 'description dfdsfds sdfadfsa sdfsadf sad asdf dsaf sdaf das asdf adsf asdfeweqreqwreqw dc ',
  },
  {
    type: 'search',
    title: 'SearchTitle1',
    id: '1111',
  },
  {
    type: 'dashboard',
    title: 'DashboardTitle2',
    id: '1111',
  },

  {
    type: 'dashboard',
    title: 'DashboardTitle1',
    id: '1111',
    summary: 'summary',
    description: 'description dfdsfds sdfadfsa sdfsadf sad asdf dsaf sdaf das asdf adsf asdfeweqreqwreqw dc ',
  },

  {
    type: 'dashboard',
    title: 'DashboardTitle1',
    id: '1111',
    summary: 'summary',
    description: 'description dfdsfds sdfadfsa sdfsadf sad asdf dsaf sdaf das asdf adsf asdfeweqreqwreqw dc ',
  },
];
const activities = [{
  timestamp: '2021-11-09T14:28:24+01:00',
  entityType: 'search',
  entityName: 'Bobs search',
  action: {
    actionUser: 'Bob',
    actionType: 'share',
    actedUser: 'you',
  },
  id: '111',
},
{
  timestamp: '2022-11-09T14:28:24+01:00',
  entityType: 'dashboard',
  entityName: 'Emmas dashboard',
  action: {
    actionUser: 'Emma',
    actionType: 'share',
    actedUser: 'you',
  },
  id: '222',
},
{
  timestamp: '2021-11-09T14:28:24+01:00',
  entityType: 'search',
  entityName: 'Bobs search',
  action: {
    actionUser: 'Bob',
    actionType: 'share',
    actedUser: 'you',
  },
  id: '111',
},
{
  timestamp: '2022-11-09T14:28:24+01:00',
  entityType: 'dashboard',
  entityName: 'Emmas dashboard',
  action: {
    actionUser: 'Emma',
    actionType: 'share',
    actedUser: 'you',
  },
  id: '222',
},
{
  timestamp: '2021-11-09T14:28:24+01:00',
  entityType: 'search',
  entityName: 'Bobs search',
  action: {
    actionUser: 'Bob',
    actionType: 'share',
    actedUser: 'you',
  },
  id: '111',
},
];

const ActionItem = ({ action, id, entityType, entityName }) => {
  return (
    <span>
      {`${action.actionUser} ${action.actionType} ${entityType}`}
      {' '}
      <ActionItemLink to={Routes.pluginRoute(typeLinkMap[entityType].link)(id)}>{entityName}</ActionItemLink>
      {' '}
      {`with ${action.actedUser}`}
    </span>
  );
};

const GettingStarted = () => {
  return (
    <>
      <PageHeader title="Getting started">
        <span>Here you can find most used content</span>
      </PageHeader>
      <FlexContainer>
        <StyledSectionComponent title="Last opened">
          <ListGroup>
            {lastOpen.map(({ type, id, title, description, summary }) => <EntityItem key={id} type={type} id={id} title={title} description={description} summary={summary} />)}
          </ListGroup>
        </StyledSectionComponent>
        <StyledSectionComponent title="Pinned items">
          <PaginatedList useQueryParameter={false} totalItems={20} pageSize={5} showPageSizeSelect={false} hideFirstAndLastPageLinks>
            <ListGroup>
              {lastOpen.map(({ type, id, title, description, summary }) => <EntityItem key={id} type={type} id={id} title={title} description={description} summary={summary} />)}
            </ListGroup>
          </PaginatedList>
        </StyledSectionComponent>
      </FlexContainer>
      <StyledSectionComponent title="Recent activity">
        <PaginatedList useQueryParameter={false} totalItems={20} pageSize={5} showPageSizeSelect={false} hideFirstAndLastPageLinks>
          <Table striped>
            <tbody>
              {
              activities.map(({ timestamp, action, id, entityName, entityType }) => {
                return (
                  <tr key={id}>
                    <td style={{ width: 100 }}><StyledLabel title={timestamp} bsStyle="primary">{relativeDifference(timestamp)}</StyledLabel></td>
                    <td>
                      <ActionItem id={id} action={action} entityName={entityName} entityType={entityType} />
                    </td>
                  </tr>
                );
              })
            }
            </tbody>
          </Table>
        </PaginatedList>

      </StyledSectionComponent>
    </>
  );
};

export default GettingStarted;
