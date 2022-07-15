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
import * as React from 'react';
import { useFormikContext } from 'formik';

import { FormikFormGroup, TimeUnitInput } from 'components/common';
import type { LookupTableCache, LookupTableCacheConfig } from 'logic/lookup-tables/types';

type Props = {
  config: LookupTableCacheConfig,
};

const CaffeineCacheFieldSet = ({ config }: Props, ref: any) => {
  const { values, setValues, errors }: { values: Partial<LookupTableCache>, setValues: any, errors: any } = useFormikContext();
  const [stateConfig, setStateConfig] = React.useState<LookupTableCacheConfig>({ ...config });

  React.useEffect(() => setStateConfig({ ...config }), [config]);

  const validateConfig = () => {
    const configErrors: any = {};

    if (!values.config.max_size) configErrors.max_size = 'Required';
    if (values.config.max_size < 0) configErrors.max_size = 'Must be 0 or a positive number';

    return configErrors;
  };

  React.useImperativeHandle(ref, () => ({
    validate: () => (validateConfig()),
  }));

  const handleUpdate = (name: string) => (value: number, unit: string, enabled: boolean) => {
    const auxConfig = { ...stateConfig };
    const auxValConfig = { ...values.config };

    auxConfig[name] = enabled ? value : 0;
    auxConfig[`${name}_unit`] = unit;
    auxValConfig[name] = enabled ? value : 0;
    auxValConfig[`${name}_unit`] = unit;

    setStateConfig(auxConfig);
    setValues({ ...values, config: auxValConfig });
  };

  return (
    <fieldset ref={ref}>
      <FormikFormGroup type="text"
                       name="config.max_size"
                       label="* Maximum entries"
                       required
                       help={errors.config?.max_size ? null : 'The limit of the number of entries the cache keeps in memory.'}
                       labelClassName="col-sm-3"
                       wrapperClassName="col-sm-9" />
      <TimeUnitInput label="Expire after access"
                     help="If enabled, entries are removed from the cache after the specified time from when they were last used."
                     update={handleUpdate('expire_after_access')}
                     name="config.expire_after_access"
                     unitName="config.expire_after_access_unit"
                     value={stateConfig.expire_after_access}
                     unit={stateConfig.expire_after_access_unit || 'SECONDS'}
                     defaultEnabled={config.expire_after_access > 0}
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9" />
      <TimeUnitInput label="Expire after write"
                     help="If enabled, entries are removed from the cache after the specified time from when they were first used."
                     update={handleUpdate('expire_after_write')}
                     name="config.expire_after_write"
                     unitName="config.expire_after_write_unit"
                     value={stateConfig.expire_after_write}
                     unit={stateConfig.expire_after_write_unit || 'SECONDS'}
                     defaultEnabled={config.expire_after_write > 0}
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9" />
    </fieldset>
  );
};

export default React.forwardRef(CaffeineCacheFieldSet);
