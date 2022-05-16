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
import type * as Immutable from 'immutable';
import { useFormikContext } from 'formik';
import { useEffect, useRef } from 'react';

import type Parameter from 'views/logic/parameters/Parameter';

type Props = {
  parameters: Immutable.Set<Parameter> | undefined,
}

const ValidateFormOnParameterChange = ({ parameters }: Props) => {
  const { validateForm } = useFormikContext();
  const mounted = useRef(false);

  useEffect(() => {
    if (mounted.current) {
      validateForm();
    } else {
      mounted.current = true;
    }
  }, [validateForm, parameters]);

  return null;
};

export default ValidateFormOnParameterChange;
