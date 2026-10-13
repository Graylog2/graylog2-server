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
import { Navigate, Route, Routes, useMatch } from 'react-router-dom';

import { Col, NavItem } from 'components/bootstrap';
import { LinkContainer } from 'components/common';
import { SectionNav, SectionNavItem } from 'components/configurations/SectionNav';
import ConfigletRow from 'pages/configurations/ConfigletRow';
import RoutePaths from 'routing/Routes';

import DataNodeConfiguration from './DataNodeConfiguration';
import { getConfigurationGroups } from './configurationFields';
import type { ConfigurationGroup } from './types';

type SectionLinkProps = { group: ConfigurationGroup; basePath: string };

const SectionLink = ({ group, basePath }: SectionLinkProps) => {
  const path = `${basePath}/${group.id}`;
  const isActive = !!useMatch(path);

  return (
    <SectionNavItem className={isActive ? 'active' : undefined}>
      <LinkContainer to={path}>
        <NavItem title={group.label} withMinHeight={false} aria-current={isActive ? 'page' : undefined}>
          {group.label}
        </NavItem>
      </LinkContainer>
    </SectionNavItem>
  );
};

type Props = React.PropsWithChildren<{ basePath: string; nodeId?: string | null }>;

const DataNodeConfigurationEditor = ({ basePath, nodeId = null, children = undefined }: Props) => {
  const configurationGroups = getConfigurationGroups(nodeId);

  return (
    <ConfigletRow className="content">
      {children && <Col xs={12}>{children}</Col>}
      <Col md={2}>
        <nav aria-label="Data Node configuration sections">
          <SectionNav>
            {configurationGroups.map((group) => (
              <SectionLink key={group.id} group={group} basePath={basePath} />
            ))}
          </SectionNav>
        </nav>
      </Col>
      <Col md={10}>
        <Routes>
          <Route index element={<Navigate to={configurationGroups[0].id} replace />} />
          {configurationGroups.map((group) => (
            <Route key={group.id} path={group.id} element={<DataNodeConfiguration group={group} nodeId={nodeId} />} />
          ))}
          <Route path="*" element={<Navigate to={RoutePaths.NOTFOUND} replace />} />
        </Routes>
      </Col>
    </ConfigletRow>
  );
};

export default DataNodeConfigurationEditor;
