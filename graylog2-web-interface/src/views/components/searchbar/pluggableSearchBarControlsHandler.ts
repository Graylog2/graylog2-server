import { merge } from 'lodash';

import type { SearchBarControl } from 'views/types';
import usePluginEntities from 'views/logic/usePluginEntities';
import type { TimeRange, NoTimeRangeOverride } from 'views/logic/queries/Query';

type FormValues = {
  timerange: TimeRange | NoTimeRangeOverride,
  streams?: Array<string>,
  queryString: string,
}

export const usePluggableInitialValues = () => {
  const pluggableSearchBarControls = usePluginEntities('views.components.searchBar');
  const existingPlugins = pluggableSearchBarControls.map((pluginData) => pluginData()).filter((pluginData) => !!pluginData) ?? [];
  const initialValuesHandler = existingPlugins.map(({ useInitialValues }) => useInitialValues)?.filter((handler) => !!handler);

  return initialValuesHandler.reduce((result, useInitialValues) => {
    // eslint-disable-next-line react-hooks/rules-of-hooks
    return { ...result, ...(useInitialValues?.() ?? {}) };
  }, {});
};

export const executePluggableSubmitHandler = async (values: FormValues, pluggableSearchBarControls: Array<() => SearchBarControl>) => {
  const pluginSubmitHandler = pluggableSearchBarControls?.map((pluginFn) => pluginFn()?.onSubmit).filter((pluginData) => !!pluginData);

  if (pluginSubmitHandler?.length) {
    await pluginSubmitHandler.forEach((onSubmit) => onSubmit(values));
  }
};

export const pluggableValidationPayload = (values: FormValues, pluggableSearchBarControls: Array<() => SearchBarControl>) => {
  const existingPluginData: Array<SearchBarControl> = pluggableSearchBarControls?.map((pluginData) => pluginData()).filter((pluginData) => !!pluginData);
  const pluggableQueryValidationPayload: Array<{ [key: string ]: any }> = existingPluginData.map(({ validationPayload }) => validationPayload?.(values)).filter((pluginValidationPayload) => !!pluginValidationPayload) ?? [];

  return merge({}, ...pluggableQueryValidationPayload);
};
