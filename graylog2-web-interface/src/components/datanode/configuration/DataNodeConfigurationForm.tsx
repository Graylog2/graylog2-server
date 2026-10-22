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
import React, { useEffect, useRef } from 'react';
import { Form, FormikProvider, useFormik } from 'formik';
import { Navigate, Route, Routes, useMatch } from 'react-router-dom';
import styled from 'styled-components';

import { Alert, Col, NavItem, Row } from 'components/bootstrap';
import { ConfirmLeaveDialog, FormSubmit, Link, LinkContainer } from 'components/common';
import { SectionNav, SectionNavItem } from 'components/configurations/SectionNav';
import ConfigletRow from 'pages/configurations/ConfigletRow';
import usePermissions from 'hooks/usePermissions';
import RoutePaths from 'routing/Routes';
import useLocation from 'routing/useLocation';
import useHistory from 'routing/useHistory';
import UserNotification from 'util/UserNotification';

import type { ConfigurationCategory, ConfigurationFormValues } from './types';
import {
  buildConfigurationFormValues,
  buildConfigurationValuesPatch,
  categoryId,
  validateConfigurationFormValues,
} from './configurationFields';
import { useUpdateManagedConfiguration } from './useManagedConfiguration';
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
  const { updateConfiguration } = useUpdateManagedConfiguration(nodeId);
  const { push } = useHistory();
  const { pathname, hash, key: locationKey } = useLocation();
  const focusedSubmit = useRef(0);
  const parameters = categories.flatMap((category) => category.parameters);
  const initialValues = buildConfigurationFormValues(parameters);
  const canDelete = isPermitted('clusterconfigentry:delete');
  const canEdit = isPermitted(['clusterconfigentry:create', 'clusterconfigentry:edit']);
  const showNavigation = categories.length > 1;
  const ignoredRoutes = [basePath, ...categories.map(({ id }) => `${basePath}/${id}`)].flatMap((path) => [
    path,
    `${path}/`,
  ]);

  const formik = useFormik<ConfigurationFormValues>({
    initialValues,
    validateOnChange: false,
    validate: (values) => validateConfigurationFormValues(values, parameters),
    onSubmit: async (values, { resetForm, setStatus }) => {
      setStatus(undefined);

      try {
        const saved = await updateConfiguration(buildConfigurationValuesPatch(formik.initialValues, values));
        resetForm({ values: buildConfigurationFormValues(saved.parameters) });
        UserNotification.success('Configuration saved.');
      } catch {
        setStatus('Could not save Data Node configuration. Your changes have been kept. Please try again.');
      }
    },
  });
  const { dirty, errors, isSubmitting, isValidating, resetForm, status, submitCount } = formik;
  const changedValues = buildConfigurationValuesPatch(formik.initialValues, formik.values);
  const restartParameters = parameters.filter(
    ({ name, requires_restart }) => requires_restart && Object.hasOwn(changedValues, name),
  );

  useEffect(() => {
    if (hash) document.getElementById(decodeURIComponent(hash.slice(1)))?.focus();
  }, [hash, locationKey, pathname]);

  useEffect(() => {
    if (submitCount === 0) focusedSubmit.current = 0;
    if (submitCount <= focusedSubmit.current || isSubmitting || isValidating) return;

    const invalidCategory = categories.find((category) => category.parameters.some(({ name }) => errors[name]));
    const invalidParameter = invalidCategory?.parameters.find(({ name }) => errors[name]);

    if (invalidCategory && invalidParameter) {
      const path = `${basePath}/${invalidCategory.id}`;

      if (pathname.replace(/\/$/, '') !== path) {
        push(path);

        return;
      }
      document.getElementById(invalidParameter.name)?.focus();
    }
    focusedSubmit.current = submitCount;
  }, [basePath, categories, errors, isSubmitting, isValidating, pathname, push, submitCount]);

  return (
    <FormikProvider value={formik}>
      <Form noValidate aria-label="Data Node configuration">
        {dirty && !isSubmitting && (
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
                  <Alert bsStyle="warning" title="Restart required after saving">
                    Changes to the following settings require restarting{' '}
                    {nodeId === null ? 'the Data Nodes' : 'this Data Node'} to take effect:
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
                        ? `Apply changes and restart ${nodeId === null ? 'Data Nodes' : 'Data Node'}`
                        : 'Apply changes'
                    }
                    isAsyncSubmit
                    isSubmitting={isSubmitting}
                    submitLoadingText="Saving changes..."
                    disabledSubmit={!dirty}
                    disabledCancel={!dirty}
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
