import { merge } from 'lodash';

import type { SearchBarControl } from 'views/types';
import type { SearchBarFormValues } from 'views/Constants';
import usePluginEntities from 'views/logic/usePluginEntities';

export const usePluggableInitialValues = () => {
  const pluggableSearchBarControls = usePluginEntities('views.components.searchBar');
  const existingPlugins = pluggableSearchBarControls.map((pluginData) => pluginData()).filter((pluginData) => !!pluginData) ?? [];
  const initialValuesHandler = existingPlugins.map(({ useInitialValues }) => useInitialValues)?.filter((handler) => !!handler);

  return initialValuesHandler.reduce((result, useInitialValues) => {
    // eslint-disable-next-line react-hooks/rules-of-hooks
    return { ...result, ...(useInitialValues?.() ?? {}) };
  }, {});
};

export const executePluggableSubmitHandler = async (values: SearchBarFormValues, pluggableSearchBarControls: Array<() => SearchBarControl>) => {
  const pluginSubmitHandler = pluggableSearchBarControls?.map((pluginFn) => pluginFn()?.onSubmit).filter((pluginData) => !!pluginData);

  if (pluginSubmitHandler?.length) {
    await pluginSubmitHandler.forEach((onSubmit) => onSubmit(values));
  }
};

export const pluggableValidationPayload = (values: SearchBarFormValues, pluggableSearchBarControls: Array<() => SearchBarControl>) => {
  const existingPluginData: Array<SearchBarControl> = pluggableSearchBarControls?.map((pluginData) => pluginData()).filter((pluginData) => !!pluginData);
  const pluggableQueryValidationPayload: Array<{ [key: string ]: any }> = existingPluginData.map(({ validationPayload }) => validationPayload?.(values)).filter((pluginValidationPayload) => !!pluginValidationPayload) ?? [];

  return merge({}, ...pluggableQueryValidationPayload);
};
