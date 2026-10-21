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

import { Alert, Col, NavItem } from 'components/bootstrap';
import { LinkContainer, Spinner } from 'components/common';
import { SectionNav, SectionNavItem } from 'components/configurations/SectionNav';
import ConfigletRow from 'pages/configurations/ConfigletRow';
import RoutePaths from 'routing/Routes';

import DataNodeConfiguration from './DataNodeConfiguration';
import { getConfigurationCategories } from './configurationFields';
import useManagedConfiguration from './useManagedConfiguration';
import type { ConfigurationCategory } from './types';

type SectionLinkProps = { category: ConfigurationCategory; basePath: string };

const SectionLink = ({ category, basePath }: SectionLinkProps) => {
  const path = `${basePath}/${category.id}`;
  const isActive = !!useMatch(path);

  return (
    <SectionNavItem className={isActive ? 'active' : undefined}>
      <LinkContainer to={path}>
        <NavItem title={category.label} withMinHeight={false} aria-current={isActive ? 'page' : undefined}>
          {category.label}
        </NavItem>
      </LinkContainer>
    </SectionNavItem>
  );
};

type Props = React.PropsWithChildren<{ basePath: string; nodeId?: string | null }>;

const DataNodeConfigurationEditor = ({ basePath, nodeId = null, children = undefined }: Props) => {
  const { data, isLoading, error } = useManagedConfiguration(nodeId);

  if (isLoading) return <Spinner text="Loading Data Node configuration..." />;
  if (error) return <Alert bsStyle="danger">Could not load Data Node configuration. Please try again.</Alert>;

  const categories = getConfigurationCategories(data?.parameters ?? []);

  if (categories.length === 0) {
    return <Alert bsStyle="info">No Data Node configuration parameters are available here.</Alert>;
  }

  return (
    <ConfigletRow className="content">
      {children && <Col xs={12}>{children}</Col>}
      <Col md={2}>
        <nav aria-label="Data Node configuration sections">
          <SectionNav>
            {categories.map((category) => (
              <SectionLink key={category.id} category={category} basePath={basePath} />
            ))}
          </SectionNav>
        </nav>
      </Col>
      <Col md={10}>
        <Routes>
          <Route index element={<Navigate to={categories[0].id} replace />} />
          {categories.map((category) => (
            <Route
              key={category.id}
              path={category.id}
              element={<DataNodeConfiguration category={category} nodeId={nodeId} />}
            />
          ))}
          <Route path="*" element={<Navigate to={RoutePaths.NOTFOUND} replace />} />
        </Routes>
      </Col>
    </ConfigletRow>
  );
};

export default DataNodeConfigurationEditor;
