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
import { merge } from 'lodash';

import UserNotification from 'util/UserNotification';
import type { SearchBarControl, CombinedSearchBarFormValues } from 'views/types';
import usePluginEntities from 'views/logic/usePluginEntities';
import type Widget from 'views/logic/widgets/Widget';
import type Query from 'views/logic/queries/Query';

const executeSafely = <T extends () => ReturnType<T>>(fn: T, errorMessage: string, fallbackResult?: ReturnType<T>): ReturnType<T> => {
  try {
    return fn();
  } catch (e) {
    // eslint-disable-next-line no-console
    console.error(`${errorMessage}: ${e}`);
    UserNotification.error(`${errorMessage}: ${e}`);
  }

  return fallbackResult;
};

const initialValues = <T>(currentQuery: T, initialValuesHandler: Array<(entity: T) => ({ [key: string]: any })>) => {
  const initialValues = initialValuesHandler.map((useInitialValues) => executeSafely(
    // eslint-disable-next-line react-hooks/rules-of-hooks
    () => useInitialValues(currentQuery),
    'An error occurred when collecting initial search bar form values from a plugin',
    {},
  ));

  return merge({}, ...initialValues);
};

export const useInitialSearchValues = (currentQuery?: Query) => {
  const pluggableSearchBarControls = usePluginEntities('views.components.searchBar') ?? [];
  const initialValuesHandler = pluggableSearchBarControls?.map((pluginFn) => pluginFn()?.useInitialSearchValues).filter((useInitialValues) => !!useInitialValues);

  return initialValues(currentQuery, initialValuesHandler)
}

export const useInitialDashboardWidgetValues = (currentWidget: Widget) => {
  const pluggableSearchBarControls = usePluginEntities('views.components.searchBar') ?? [];
  const initialValuesHandler = pluggableSearchBarControls?.map((pluginFn) => pluginFn()?.useInitialDashboardWidgetValues).filter((useInitialValues) => !!useInitialValues);

  return initialValues(currentWidget, initialValuesHandler)
}

const executeSubmitHandler = async <T>(values: CombinedSearchBarFormValues, submitHandlers: Array<(values: CombinedSearchBarFormValues, entity?: T) => Promise<T>>, currentEntity?: T): Promise<T> => {
  let updatedEntity = currentEntity;

  for (const submitHandler of submitHandlers) {
    const entityWithPluginData = await submitHandler(values, updatedEntity).catch((e) => {
      const errorMessage = `An error occurred when executing a submit handler from a plugin: ${e}`;
      // eslint-disable-next-line no-console
      console.error(errorMessage);
      UserNotification.error(errorMessage);

      return updatedEntity
    });

    if (entityWithPluginData) {
      updatedEntity = entityWithPluginData
    }
  }

  return updatedEntity;
};

export const executeSearchSubmitHandler = (values: CombinedSearchBarFormValues, pluggableSearchBarControls: Array<() => SearchBarControl>, currentQuery?: Query) => {
  const pluginSubmitHandlers = pluggableSearchBarControls?.map((pluginFn) => pluginFn()?.onSearchSubmit).filter((pluginData) => !!pluginData);

  return executeSubmitHandler(values, pluginSubmitHandlers, currentQuery);
}

export const executeDashboardWidgetSubmitHandler = (values: CombinedSearchBarFormValues, pluggableSearchBarControls: Array<() => SearchBarControl>, currentWidget?: Widget) => {
  const pluginSubmitHandlers = pluggableSearchBarControls?.map((pluginFn) => pluginFn()?.onDashboardWidgetSubmit).filter((pluginData) => !!pluginData);

  return executeSubmitHandler(values, pluginSubmitHandlers, currentWidget);
}

export const pluggableValidationPayload = (values: CombinedSearchBarFormValues, pluggableSearchBarControls: Array<() => SearchBarControl> = []) => {
  const validationPayloadHandler = pluggableSearchBarControls.map((pluginFn) => pluginFn()?.validationPayload).filter((validationPayloadFn) => !!validationPayloadFn);
  const validationPayload: Array<{ [key: string ]: any }> = validationPayloadHandler.map((validationPayloadFn) => executeSafely(
    () => validationPayloadFn(values),
    'An error occurred when preparing search bar validation for a plugin',
    {},
  ));

  return merge({}, ...validationPayload);
};

export const validatePluggableValues = (values: CombinedSearchBarFormValues, pluggableSearchBarControls: Array<() => SearchBarControl> = []) => {
  const validationHandler = pluggableSearchBarControls.map((pluginFn) => pluginFn()?.onValidate).filter((onValidate) => !!onValidate);

  const errors = validationHandler.map((onValidate) => executeSafely(
    () => onValidate(values),
    'An error occurred when validating search bar values from a plugin',
    {},
  ));

  return merge({}, ...errors);
};
