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
import { useTranslation } from 'react-i18next';
import { FastField } from 'formik';

import { Input } from 'components/bootstrap';
import { TimezoneSelect } from 'components/common';

const TimezoneFormGroup = () => {
  const { t } = useTranslation('users');

  return (
    <FastField name="timezone">
      {({ field: { name, value, onChange } }) => (
        <Input
          id="timezone-select"
          label={t('user_fields.time_zone')}
          help={t('user_form.time_zone_help')}
          labelClassName="col-sm-3"
          wrapperClassName="col-sm-9">
          <TimezoneSelect
            className="timezone-select"
            value={value || t('time.browser_time_zone', { ns: 'common' })}
            name="timezone"
            onChange={(newValue) => onChange({ target: { name, value: newValue } })}
          />
        </Input>
      )}
    </FastField>
  );
};

export default TimezoneFormGroup;
