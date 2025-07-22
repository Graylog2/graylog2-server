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
import { Form, Formik } from 'formik';
import { useState } from 'react';
import moment from 'moment';

import { FormikInput, TimeUnitInput } from 'components/common';
import { Button, ButtonToolbar, Checkbox, Modal } from 'components/bootstrap';
import type { ClientCertFormValues } from 'components/datanode/hooks/useCreateDataNodeClientCert';
import useCreateDataNodeClientCert from 'components/datanode/hooks/useCreateDataNodeClientCert';
import ClientCertificateView from 'components/datanode/client-certificate/ClientCertificateView';

import { TIME_UNITS_UPPER } from '../Constants';
import ModalSubmit from '../../common/ModalSubmit';

type Props = {
  onCancel: () => void;
};

const ClientCertForm = ({ onCancel }: Props) => {
  const [clientCerts, setClientCerts] = useState(null);
  const [isUnencrypted, setIsUnencrypted] = useState(false);
  const { onCreateClientCert } = useCreateDataNodeClientCert();

  const onSubmit = (formValues: ClientCertFormValues) => {
    const { lifetimeValue, lifetimeUnit, ...restValues } = formValues;
    const requestValues = {
      ...restValues,
      certificate_lifetime: moment
        .duration(lifetimeValue, lifetimeUnit as unknown as moment.unitOfTime.DurationConstructor)
        .toJSON(),
    };

    return onCreateClientCert(requestValues)
      .then((certs) => setClientCerts(certs))
      .catch(() => {});
  };

  const onToggleUnencrypted = (setFieldValue: (field: string, value: any) => void) => {
    setIsUnencrypted(!isUnencrypted);
    setFieldValue('password', null);
  };

  return (
    <>
      <Modal.Header>
        <Modal.Title>Create client certificate</Modal.Title>
      </Modal.Header>
      {!clientCerts && (
        <Formik
          initialValues={
            {
              principal: '',
              role: 'all_access',
              password: null,
              lifetimeValue: 30,
              lifetimeUnit: 'days',
            } as ClientCertFormValues
          }
          onSubmit={(formValues: ClientCertFormValues) => onSubmit(formValues)}>
          {({ isSubmitting, values, setFieldValue }) => (
            <Form>
              <Modal.Body>
                <FormikInput id="principal" placeholder="principal" name="principal" label="Principal" required />
                <FormikInput
                  id="role"
                  placeholder="role"
                  name="role"
                  help="Represent OpenSearch roles mapping."
                  label="Role"
                  required
                />
                <Checkbox
                  type="checkbox"
                  id="client_certificate_unencrypted"
                  name="client_certificate_unencrypted"
                  checked={isUnencrypted}
                  onChange={() => onToggleUnencrypted(setFieldValue)}>
                  Unencrypted
                </Checkbox>
                <FormikInput
                  id="password"
                  placeholder="*******"
                  name="password"
                  type="password"
                  label="Password"
                  disabled={isUnencrypted}
                  required={!isUnencrypted}
                />
                <TimeUnitInput
                  label="Certificate Lifetime"
                  update={(value, unit) => {
                    setFieldValue('lifetimeValue', value);
                    setFieldValue('lifetimeUnit', unit);
                  }}
                  value={values.lifetimeValue}
                  unit={values.lifetimeUnit.toLocaleUpperCase()}
                  enabled
                  hideCheckbox
                  units={TIME_UNITS_UPPER}
                />
              </Modal.Body>
              <Modal.Footer>
                <ModalSubmit
                  onCancel={() => onCancel()}
                  isSubmitting={isSubmitting}
                  isAsyncSubmit
                  submitButtonText="Create Certificate"
                  submitLoadingText="Creating certificate..."
                />
              </Modal.Footer>
            </Form>
          )}
        </Formik>
      )}
      {clientCerts && (
        <>
          <Modal.Body>
            <ClientCertificateView clientCerts={clientCerts} />
          </Modal.Body>
          <Modal.Footer>
            <ButtonToolbar>
              <Button bsStyle="success" onClick={() => onCancel()}>
                Close
              </Button>
            </ButtonToolbar>
          </Modal.Footer>
        </>
      )}
    </>
  );
};

export default ClientCertForm;
