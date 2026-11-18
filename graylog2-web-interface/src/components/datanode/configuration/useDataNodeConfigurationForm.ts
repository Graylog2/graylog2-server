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
import { useEffect, useRef } from 'react';
import { useFormik } from 'formik';
import isEqual from 'lodash/isEqual';

import useLocation from 'routing/useLocation';
import useHistory from 'routing/useHistory';
import UserNotification from 'util/UserNotification';
import extractErrorMessage from 'util/extractErrorMessage';

import type { ConfigurationCategory, ConfigurationValues } from './types';
import {
  buildConfigurationValues,
  buildConfigurationValuesPatch,
  validateConfigurationValues,
} from './configurationFields';
import { useRestartDataNodes, useUpdateManagedConfiguration } from './useManagedConfiguration';
import useDataNodeRestartProgress from './useDataNodeRestartProgress';

import { rollingRestartStartError } from '../opensearch-upgrade/hooks/useOpenSearchRollingRestart';

type Options = {
  categories: Array<ConfigurationCategory>;
  nodeId: string | null;
  basePath: string;
};

const useDataNodeConfigurationForm = ({ categories, nodeId, basePath }: Options) => {
  const { updateConfiguration } = useUpdateManagedConfiguration(nodeId);
  const { restartDataNodes } = useRestartDataNodes(nodeId);
  const { isRestarting, progress: restartProgress, markRestartRequested } = useDataNodeRestartProgress(nodeId);
  const { push } = useHistory();
  const { pathname, hash, key: locationKey } = useLocation();
  const lastFocusedSubmit = useRef(0);
  const saveOnlyRequested = useRef(false);
  const parameters = categories.flatMap((category) => category.parameters);
  const serverValues = buildConfigurationValues(parameters);

  const formik = useFormik<ConfigurationValues>({
    initialValues: serverValues,
    validateOnChange: false,
    validate: (values) => validateConfigurationValues(formik.initialValues, values, parameters),
    onSubmit: async (draftValues, { resetForm }) => {
      const saveOnly = saveOnlyRequested.current;
      const changes = buildConfigurationValuesPatch(formik.initialValues, draftValues, parameters);
      const restartTarget = nodeId === null ? 'the Data Nodes' : 'this Data Node';

      let saved;
      try {
        saved = await updateConfiguration(changes);
        resetForm({ values: buildConfigurationValues(saved.parameters) });
      } catch (error) {
        const details =
          error instanceof Error || typeof error === 'string' ? extractErrorMessage(error).trim() : undefined;
        UserNotification.error(
          [details, 'Please try again.'].filter(Boolean).join(' '),
          'Could not save Data Node configuration',
        );

        return;
      }

      // Restart for what was just changed, and for values saved earlier which the Data Nodes don't run with yet.
      const changedRestartParameter = parameters.some(
        ({ name, requires_restart }) => requires_restart && Object.hasOwn(changes, name),
      );
      const needsRestart =
        changedRestartParameter ||
        saved.restart_required ||
        saved.parameters.some(({ pending_restart }) => pending_restart);

      if (saveOnly || !needsRestart) {
        UserNotification.success('Configuration saved.');

        return;
      }

      if (isRestarting) {
        // Saved values are picked up by the restart which is running already or by the next one
        UserNotification.success(
          `Configuration saved. ${nodeId === null ? 'The Data Nodes are' : 'This Data Node is'} restarting already.`,
        );

        return;
      }

      try {
        await restartDataNodes();
        markRestartRequested();
        UserNotification.success(`Configuration saved. Restarting ${restartTarget}.`);
      } catch (error) {
        UserNotification.error(
          rollingRestartStartError(error).message,
          `Configuration saved, but ${restartTarget} could not be restarted`,
        );
      }
    },
  });
  const { values, errors, isSubmitting, isValidating, resetForm, submitCount } = formik;
  const changedValues = buildConfigurationValuesPatch(formik.initialValues, values, parameters);
  const hasUnsavedChanges = Object.keys(changedValues).length > 0;
  // Changed in the form, or saved before but not in use yet: the server tracks the latter across reloads.
  const restartParameters = parameters.filter(
    ({ name, requires_restart, pending_restart }) =>
      (requires_restart && Object.hasOwn(changedValues, name)) || pending_restart,
  );
  const hasUnsavedRestartChanges = restartParameters.some(({ name }) => Object.hasOwn(changedValues, name));

  const saveChangesWithoutRestart = async () => {
    saveOnlyRequested.current = true;
    try {
      await formik.submitForm();
    } finally {
      saveOnlyRequested.current = false;
    }
  };

  useEffect(() => {
    if (!hasUnsavedChanges && !isSubmitting && !isEqual(values, serverValues)) {
      resetForm({ values: serverValues });
    }
  }, [hasUnsavedChanges, isSubmitting, values, serverValues, resetForm]);

  useEffect(() => {
    if (hash) document.getElementById(decodeURIComponent(hash.slice(1)))?.focus();
  }, [hash, locationKey, pathname]);

  useEffect(() => {
    if (submitCount === 0) lastFocusedSubmit.current = 0;
    if (submitCount <= lastFocusedSubmit.current || isSubmitting || isValidating) return;

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
    lastFocusedSubmit.current = submitCount;
  }, [basePath, categories, errors, isSubmitting, isValidating, pathname, push, submitCount]);

  return {
    formik,
    hasUnsavedChanges,
    restartParameters,
    hasUnsavedRestartChanges,
    saveChangesWithoutRestart,
    isRestarting,
    restartProgress,
  };
};

export default useDataNodeConfigurationForm;
