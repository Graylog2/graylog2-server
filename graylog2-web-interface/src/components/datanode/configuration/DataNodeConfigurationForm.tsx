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
import React, { useState } from 'react';
import { Form, FormikProvider } from 'formik';
import { Navigate, Route, Routes, useMatch } from 'react-router-dom';

import { Alert, Button, Col, NavItem, Row } from 'components/bootstrap';
import { ConfirmLeaveDialog, FormSubmit, LinkContainer } from 'components/common';
import { SectionNav, SectionNavItem } from 'components/configurations/SectionNav';
import usePermissions from 'hooks/usePermissions';
import RoutePaths from 'routing/Routes';

import type { ConfigurationCategory } from './types';
import useDataNodeConfigurationForm from './useDataNodeConfigurationForm';
import DataNodeConfigurationSection from './DataNodeConfigurationSection';
import ConfigurationPrecedenceHelp from './ConfigurationPrecedenceHelp';
import DataNodeRestartProgress from './DataNodeRestartProgress';
import DataNodeRestartRequiredAlert from './DataNodeRestartRequiredAlert';

import ForceStartConfirmDialog from '../opensearch-upgrade/ForceStartConfirmDialog';

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

const restartAlertTitle = (nodeId: string | null, paused: boolean) => {
  if (paused) return 'Data Node restart paused';

  return nodeId === null ? 'Restarting Data Nodes' : 'Restarting Data Node';
};

type Props = React.PropsWithChildren<{
  categories: Array<ConfigurationCategory>;
  nodeId: string | null;
  basePath: string;
}>;

const DataNodeConfigurationForm = ({ categories, nodeId, basePath, children = undefined }: Props) => {
  const { isPermitted } = usePermissions();
  const {
    formik,
    hasUnsavedChanges,
    restartParameters,
    hasUnsavedRestartChanges,
    pendingSharedRestart,
    isRestartPending,
    saveChangesWithoutRestart,
    canRestart,
    isRestarting,
    isRestartPaused,
    restartProgress,
    forceRestart,
  } = useDataNodeConfigurationForm({ categories, nodeId, basePath });
  const { isSubmitting, resetForm } = formik;
  const [selectedUnits, setSelectedUnits] = useState<Record<string, string>>({});
  const selectUnit = (name: string, unit: string) => setSelectedUnits((units) => ({ ...units, [name]: unit }));
  const canDelete = isPermitted('clusterconfigentry:delete');
  const canEdit = isPermitted(['clusterconfigentry:create', 'clusterconfigentry:edit']);
  const showNavigation = categories.length > 1;
  const nodeLabel = nodeId === null ? 'Data Nodes' : 'Data Node';
  const offerRestart = canRestart && isRestartPending && !isRestarting;
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
        <Row className="content">
          <Col xs={12}>
            {children}
            <ConfigurationPrecedenceHelp />
          </Col>
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
                <Routes>
                  <Route index element={<Navigate to={categories[0].id} replace />} />
                  {categories.map((category) => (
                    <Route
                      key={category.id}
                      path={category.id}
                      element={
                        <DataNodeConfigurationSection
                          category={category}
                          canEdit={canEdit}
                          canDelete={canDelete}
                          selectedUnits={selectedUnits}
                          onSelectUnit={selectUnit}
                        />
                      }
                    />
                  ))}
                  <Route path="*" element={<Navigate to={RoutePaths.NOTFOUND} replace />} />
                </Routes>
                {isRestarting && (
                  <Alert
                    bsStyle={isRestartPaused ? 'warning' : 'info'}
                    title={restartAlertTitle(nodeId, isRestartPaused)}>
                    <DataNodeRestartProgress progress={restartProgress} paused={isRestartPaused} />
                  </Alert>
                )}
                {canEdit && !isRestarting && isRestartPending && (
                  <DataNodeRestartRequiredAlert
                    nodeId={nodeId}
                    basePath={basePath}
                    restartParameters={restartParameters}
                    hasUnsavedRestartChanges={hasUnsavedRestartChanges}
                    pendingSharedRestart={pendingSharedRestart}
                    canRestart={canRestart}
                  />
                )}
                {canEdit && (
                  <FormSubmit
                    submitButtonText={
                      offerRestart
                        ? `${hasUnsavedChanges ? 'Apply changes and restart' : 'Restart'} ${nodeLabel}`
                        : 'Apply changes'
                    }
                    centerCol={
                      canRestart &&
                      isRestartPending && (
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
                    disabledSubmit={!hasUnsavedChanges && !offerRestart}
                    disabledCancel={!hasUnsavedChanges}
                    onCancel={() => resetForm()}
                  />
                )}
              </Col>
            </Row>
          </Col>
        </Row>
      </Form>
      {forceRestart.failedChecks.length > 0 && (
        <ForceStartConfirmDialog
          title="Restart the Data Nodes anyway?"
          intro="The configuration has been saved, but the checks before a rolling restart did not pass."
          confirmText="Restart Data Nodes anyway"
          submittingText="Restarting..."
          failedChecks={forceRestart.failedChecks}
          isSubmitting={forceRestart.isSubmitting}
          onCancel={forceRestart.cancel}
          onConfirm={forceRestart.confirm}
        />
      )}
    </FormikProvider>
  );
};

export default DataNodeConfigurationForm;
