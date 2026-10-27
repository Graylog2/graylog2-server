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
import { Form, FormikProvider } from 'formik';
import { Navigate, Route, Routes, useMatch } from 'react-router-dom';
import styled from 'styled-components';

import { Alert, Button, Col, NavItem, Row } from 'components/bootstrap';
import { ConfirmLeaveDialog, FormSubmit, Link, LinkContainer } from 'components/common';
import { SectionNav, SectionNavItem } from 'components/configurations/SectionNav';
import ConfigletRow from 'pages/configurations/ConfigletRow';
import usePermissions from 'hooks/usePermissions';
import RoutePaths from 'routing/Routes';

import type { ConfigurationCategory } from './types';
import { categoryId } from './configurationFields';
import useDataNodeConfigurationForm from './useDataNodeConfigurationForm';
import DataNodeConfiguration from './DataNodeConfiguration';

const RestartSettingsList = styled.ul`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacings.xs};
  margin-top: ${({ theme }) => theme.spacings.sm};
  padding-left: ${({ theme }) => theme.spacings.lg};
  list-style-type: disc;
`;

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

type Props = React.PropsWithChildren<{
  categories: Array<ConfigurationCategory>;
  nodeId: string | null;
  basePath: string;
}>;

const DataNodeConfigurationForm = ({ categories, nodeId, basePath, children = undefined }: Props) => {
  const { isPermitted } = usePermissions();
  const { formik, hasUnsavedChanges, restartParameters, hasUnsavedRestartChanges, saveChangesWithoutRestart } =
    useDataNodeConfigurationForm({ categories, nodeId, basePath });
  const { isSubmitting, resetForm, status } = formik;
  const canDelete = isPermitted('clusterconfigentry:delete');
  const canEdit = isPermitted(['clusterconfigentry:create', 'clusterconfigentry:edit']);
  const showNavigation = categories.length > 1;
  const nodeLabel = nodeId === null ? 'Data Nodes' : 'Data Node';
  const restartTarget = nodeId === null ? 'the Data Nodes' : 'this Data Node';
  const ignoredRoutes = [basePath, ...categories.map(({ id }) => `${basePath}/${id}`)].flatMap((path) => [
    path,
    `${path}/`,
  ]);

  return (
    <FormikProvider value={formik}>
      <Form noValidate aria-label="Data Node configuration">
        {hasUnsavedChanges && !isSubmitting && (
          <ConfirmLeaveDialog question="Discard your unsaved configuration changes?" ignoredRoutes={ignoredRoutes} />
        )}
        <ConfigletRow className="content">
          {children && <Col xs={12}>{children}</Col>}
          {showNavigation && (
            <Col md={2}>
              <nav aria-label="Data Node configuration sections">
                <SectionNav>
                  {categories.map((category) => (
                    <SectionLink key={category.id} category={category} basePath={basePath} />
                  ))}
                </SectionNav>
              </nav>
            </Col>
          )}
          <Col md={showNavigation ? 10 : 12}>
            <Row>
              <Col md={8}>
                {status && <Alert bsStyle="danger">{status}</Alert>}
                <Routes>
                  <Route index element={<Navigate to={categories[0].id} replace />} />
                  {categories.map((category) => (
                    <Route
                      key={category.id}
                      path={category.id}
                      element={<DataNodeConfiguration category={category} canEdit={canEdit} canDelete={canDelete} />}
                    />
                  ))}
                  <Route path="*" element={<Navigate to={RoutePaths.NOTFOUND} replace />} />
                </Routes>
                {canEdit && restartParameters.length > 0 && (
                  <Alert
                    bsStyle="warning"
                    title={
                      hasUnsavedRestartChanges ? 'Restart required after saving' : 'Saved changes require a restart'
                    }>
                    {hasUnsavedRestartChanges
                      ? `Changes to the following settings require restarting ${restartTarget} to take effect:`
                      : `The following settings have been saved. Restart ${restartTarget} for them to take effect:`}
                    <RestartSettingsList>
                      {restartParameters.map(({ name, label, category }) => (
                        <li key={name}>
                          <Link to={`${basePath}/${categoryId(category)}#${encodeURIComponent(name)}`}>{label}</Link>
                        </li>
                      ))}
                    </RestartSettingsList>
                  </Alert>
                )}
                {canEdit && (
                  <FormSubmit
                    submitButtonText={
                      restartParameters.length > 0
                        ? `${hasUnsavedChanges ? 'Apply changes and restart' : 'Restart'} ${nodeLabel}`
                        : 'Apply changes'
                    }
                    centerCol={
                      restartParameters.length > 0 && (
                        <Button
                          type="button"
                          disabled={!hasUnsavedChanges || isSubmitting}
                          onClick={saveChangesWithoutRestart}>
                          Save changes
                        </Button>
                      )
                    }
                    isAsyncSubmit
                    isSubmitting={isSubmitting}
                    submitLoadingText="Saving changes..."
                    disabledSubmit={!hasUnsavedChanges && restartParameters.length === 0}
                    disabledCancel={!hasUnsavedChanges}
                    onCancel={() => resetForm()}
                  />
                )}
              </Col>
            </Row>
          </Col>
        </ConfigletRow>
      </Form>
    </FormikProvider>
  );
};

export default DataNodeConfigurationForm;
