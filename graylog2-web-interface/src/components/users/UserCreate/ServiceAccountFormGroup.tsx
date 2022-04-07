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
import { useRef } from 'react';
import { useFormikContext } from 'formik';

import { Input, BootstrapModalConfirm } from 'components/bootstrap';
import { getValueFromInput } from 'util/FormsUtils';
import type { UserJSON } from 'logic/users/User';

const ServiceAccountFormGroup = () => {
  const confirmationModalRef = useRef<typeof BootstrapModalConfirm>();
  const { setFieldValue, values } = useFormikContext<UserJSON>();

  const onValueChange = (newValue) => {
    const serviceAccountNewValue = getValueFromInput(newValue.target);

    if (serviceAccountNewValue) {
      confirmationModalRef.current.open();
    } else {
      setFieldValue('service_account', serviceAccountNewValue);
    }
  };

  const handleCheckServiceAccount = () => {
    setFieldValue('service_account', true);
    confirmationModalRef.current.close();
  };

  const handleCancel = () => {
    setFieldValue('service_account', false);
    confirmationModalRef.current.close();
  };

  return (
    <>
      <Input id="service-account-controls"
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9"
             label="Service Account">
        <Input label="service account"
               type="checkbox"
               id="service_account"
               name="service_account"
               checked={values?.service_account ?? false}
               help="When checked, the account will be set as Service account and self-edit is not allowed."
               onChange={(newValue) => onValueChange(newValue)} />
      </Input>
      <BootstrapModalConfirm ref={confirmationModalRef}
                             title="Are you sure?"
                             onConfirm={handleCheckServiceAccount}
                             onCancel={handleCancel}>
        Setting this account as Service Account will make it not self editable, this implies that login with this user will also be disabled. Do you wish to proceed?
      </BootstrapModalConfirm>
    </>
  );
};

export default ServiceAccountFormGroup;
