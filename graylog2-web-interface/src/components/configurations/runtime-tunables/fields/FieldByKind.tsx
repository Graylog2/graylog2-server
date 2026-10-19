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

import ScalarField from './ScalarField';
import DurationField from './DurationField';
import SecretField from './SecretField';

import { fieldKind } from '../schema';
import type { RuntimeTunableFieldProps } from '../types';

const FieldByKind = (props: RuntimeTunableFieldProps) => {
  switch (fieldKind(props.schema)) {
    case 'duration':
      return <DurationField {...props} />;
    case 'secret':
      return <SecretField {...props} />;
    default:
      return <ScalarField {...props} />;
  }
};

export default FieldByKind;
