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

import { FormikFormGroup } from 'components/common';

const FirstNameFormGroup = () => {
  const { t } = useTranslation('users');

  return (
    <FormikFormGroup
      label={t('user_fields.first_name')}
      name="first_name"
      maxLength={200}
      required
      help={t('user_form.first_name_help')}
    />
  );
};

export default FirstNameFormGroup;
