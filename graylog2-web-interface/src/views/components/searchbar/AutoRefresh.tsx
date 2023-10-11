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

import { useEffect, useCallback } from 'react';
import { useFormikContext } from 'formik';

import { RefreshActions } from 'views/stores/RefreshStore';

const handelSubmit = (isValid, isValidating, isSubmitting, submitForm) => {
  if (isValid && !isValidating && !isSubmitting) {
    return Promise.resolve(submitForm());
  }

  return Promise.resolve();
};

const AutoRefresh = () => {
  const { submitForm, isValid, isValidating, isSubmitting } = useFormikContext();

  useEffect(() => RefreshActions.refresh.listen(() => {
    RefreshActions.refresh.promise(handelSubmit(isValid, isValidating, isSubmitting, submitForm));
  }), [isSubmitting, isValid, isValidating, submitForm]);

  return null;
};

export default AutoRefresh;
