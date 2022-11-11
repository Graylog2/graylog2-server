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
  LinkToNode,
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

const FlexContainer = styled.div`
  display: flex;
  align-items: stretch;
  flex-wrap: wrap;
  gap: 45px;
`;

const VisualizationContainer = styled(Col)`
  min-height: 200px;
  min-width: 33%;
`;

const StyledSectionComponent = styled(SectionComponent)`
  flex-grow: 1;
`;
const typeLinkMap = {
  dashboard: { link: 'DASHBOARDS_VIEWID', icon: 'grid-horizontal' },
  search: { link: 'SEARCH_VIEWID', icon: 'folder-magnifying-glass' },
};

const EntityItem = ({ type, title, id }) => {
  return (
    <ListGroupItem>
      <Icon name="search" />
      <Link to={Routes.pluginRoute(typeLinkMap[type].link)(id)}>{title}</Link>
    </ListGroupItem>
  );
};

const StyledLabel = styled(Label)`
  cursor: default;
`;
const lastOpen = [
  {
    type: 'dashboard',
    title: 'DashboardTitle1',
    id: '1111',
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
];
const activities = [{
  timestamp: '2021-11-09T14:28:24+01:00',
  title: 'someone action made action with smth',
  id: '111',
},
{
  timestamp: '2022-11-09T14:28:24+01:00',
  title: 'someone action made action with smth',
  id: '222',
},
];

const GettingStarted = () => {
  console.log('!!!!!!!', relativeDifference('2021-11-09T14:28:24+01:00'));

  return (
    <>
      <PageHeader title="Getting started">
        <span>Some amazing description</span>
      </PageHeader>
      <FlexContainer>
        <StyledSectionComponent title="Last opened">
          <ListGroup>
            {lastOpen.map(({ type, id, title }) => <EntityItem key={id} type={type} id={id} title={title} />)}
          </ListGroup>
        </StyledSectionComponent>
        <StyledSectionComponent title="Pinned dashboard">
          <ListGroup>
            {lastOpen.map(({ type, id, title }) => <EntityItem key={id} type={type} id={id} title={title} />)}
          </ListGroup>
        </StyledSectionComponent>
      </FlexContainer>
      <StyledSectionComponent title="Recent activity">
        <Table striped>
          <tbody>
            {
            activities.map(({ timestamp, title, id }) => {
              return (
                <tr key={id}>
                  <td><StyledLabel title={timestamp}>{relativeDifference(timestamp)}</StyledLabel></td>
                  <td>
                    {title}
                  </td>
                </tr>
              );
            })
          }
          </tbody>
        </Table>
      </StyledSectionComponent>
    </>
  );
};

export default GettingStarted;
