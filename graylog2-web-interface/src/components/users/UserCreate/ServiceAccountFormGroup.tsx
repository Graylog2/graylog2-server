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

import { Input } from 'components/bootstrap';
import { FormikFormGroup } from 'components/common';

const ServiceAccountFormGroup = () => (
  <Input id="service-account-controls"
         labelClassName="col-sm-3"
         wrapperClassName="col-sm-9"
         label="Service Account">
    <FormikFormGroup label="service account"
                     type="checkbox"
                     name="service_account"
                     help="When checked, the account will be set as Service account." />
  </Input>
);

export default ServiceAccountFormGroup;
