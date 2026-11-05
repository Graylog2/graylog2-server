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
import { useEffect, useImperativeHandle, useState } from 'react';
import { useFormikContext } from 'formik';
import { useTranslation } from 'react-i18next';

import { Input } from 'components/bootstrap';
import { FormikFormGroup, TimeUnitInput } from 'components/common';
import type { LookupTableCache, LookupTableCacheConfig } from 'logic/lookup-tables/types';
import { getValueFromInput } from 'util/FormsUtils';

type Props = {
  config: LookupTableCacheConfig;
};

const CaffeineCacheFieldSet = ({ config }: Props, ref: any) => {
  const { t } = useTranslation('lookup_tables');
  const { values, setValues, errors }: { values: Partial<LookupTableCache>; setValues: any; errors: any } =
    useFormikContext();
  const [stateConfig, setStateConfig] = useState<LookupTableCacheConfig>({ ...config });

  // eslint-disable-next-line react-hooks/set-state-in-effect
  useEffect(() => setStateConfig({ ...config }), [config]);

  const validateConfig = () => {
    const configErrors: any = {};

    if (!values.config.max_size) configErrors.max_size = t('caffeine_fields.required');
    if (values.config.max_size < 0) configErrors.max_size = t('caffeine_fields.must_be_positive');

    return configErrors;
  };

  useImperativeHandle(ref, () => ({
    validate: () => validateConfig(),
  }));

  const handleIgnoreNullChange = (event) => {
    const ignoreValue = getValueFromInput(event.target);

    const valConfig = { ...values.config, ignore_null: ignoreValue as boolean };
    setStateConfig(valConfig);
    setValues({ ...values, config: valConfig });
  };

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
      <FormikFormGroup
        type="text"
        name="config.max_size"
        label={t('caffeine_fields.max_size_label')}
        required
        help={errors.config?.max_size ? null : t('caffeine_fields.max_size_help')}
        labelClassName="col-sm-3"
        wrapperClassName="col-sm-9"
      />
      <TimeUnitInput
        label={t('caffeine_doc.expire_access_title')}
        help={t('caffeine_fields.expire_access_help')}
        update={handleUpdate('expire_after_access')}
        name="config.expire_after_access"
        unitName="config.expire_after_access_unit"
        value={stateConfig.expire_after_access}
        unit={stateConfig.expire_after_access_unit || 'SECONDS'}
        defaultEnabled={config.expire_after_access > 0}
        labelClassName="col-sm-3"
        wrapperClassName="col-sm-9"
      />
      <TimeUnitInput
        label={t('caffeine_doc.expire_write_title')}
        help={t('caffeine_fields.expire_write_help')}
        update={handleUpdate('expire_after_write')}
        name="config.expire_after_write"
        unitName="config.expire_after_write_unit"
        value={stateConfig.expire_after_write}
        unit={stateConfig.expire_after_write_unit || 'SECONDS'}
        defaultEnabled={config.expire_after_write > 0}
        labelClassName="col-sm-3"
        wrapperClassName="col-sm-9"
      />
      <Input
        type="checkbox"
        id="ignore_null"
        name="ignore_null"
        label={t('caffeine_fields.ignore_null_label')}
        checked={stateConfig.ignore_null}
        onChange={handleIgnoreNullChange}
        help={t('caffeine_fields.ignore_null_help')}
        wrapperClassName="col-md-offset-3 col-md-9"
      />
      <TimeUnitInput
        label={t('caffeine_fields.ttl_empty_label')}
        help={t('caffeine_fields.ttl_empty_help')}
        update={handleUpdate('ttl_empty')}
        name="config.ttl_empty"
        unitName="config.ttl_empty_unit"
        value={stateConfig.ttl_empty}
        unit={stateConfig.ttl_empty_unit || 'SECONDS'}
        enabled={!stateConfig.ignore_null}
        hideCheckbox
        labelClassName="col-sm-3"
        wrapperClassName="col-sm-9"
      />
    </fieldset>
  );
};

export default React.forwardRef(CaffeineCacheFieldSet);
