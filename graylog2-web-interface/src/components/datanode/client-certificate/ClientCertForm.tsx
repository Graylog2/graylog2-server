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
import { Field, Form, Formik } from 'formik';
import { useState } from 'react';
import capitalize from 'lodash/capitalize';

import { FormikInput, TimeUnitInput } from 'components/common';
import { Input, Button, ButtonToolbar, Modal } from 'components/bootstrap';
import type { ClientCertFormValues } from 'components/datanode/hooks/useCreateDataNodeClientCert';
import useCreateDataNodeClientCert from 'components/datanode/hooks/useCreateDataNodeClientCert';
import ClientCertificateView from 'components/datanode/client-certificate/ClientCertificateView';
import Select from 'components/common/Select';

import { TIME_UNITS_UPPER } from '../Constants';
import ModalSubmit from '../../common/ModalSubmit';

type Props = {
  onCancel: () => void,
};

const ClientCertForm = ({ onCancel }: Props) => {
  const [clientCerts, setClientCerts] = useState(null);
  const { onCreateClientCert } = useCreateDataNodeClientCert();
  const onSubmit = (formValues: ClientCertFormValues) => onCreateClientCert(formValues).then((certs) => setClientCerts(certs))
    .catch(() => {});

  return (
    <>
      <Modal.Header closeButton>
        <Modal.Title>Create client certificate</Modal.Title>
      </Modal.Header>
      {!clientCerts && (
        <Formik initialValues={{ 
          principal: '',
          role: 'all_access',
          password: '',
          lifetimeValue: 30,
          lifetimeUnit: 'days',
          mode: 'AUTOMATIC',
        } as ClientCertFormValues} onSubmit={(formValues: ClientCertFormValues) => onSubmit(formValues)}>
          {({ isSubmitting, values, setFieldValue }) => (
            <Form>
              <Modal.Body>
                <FormikInput id="principal"
                             placeholder="principal"
                             name="principal"
                             label="Principal"
                             required />
                <FormikInput id="role"
                             placeholder="role"
                             name="role"
                             help="Represent OpenSearch roles mapping."
                             label="Role"
                             required />
                <FormikInput id="password"
                             placeholder="*******"
                             name="password"
                             type="password"
                             label="Password"
                             required />
                <Field name="mode">
                  {({ field: { name, value, onChange } }) => (
                    <Input id={name} label="Certificate Renewal Mode">
                      <Select options={['AUTOMATIC', 'MANUAL'].map((mode) => ({ label: capitalize(mode), value: mode }))}
                              clearable={false}
                              name={name}
                              value={value ?? 'AUTOMATIC'}
                              aria-label="Select certificate renewal mode"
                              size="small"
                              onChange={(newValue) => onChange({ target: { name, value: newValue } })} />
                    </Input>
                  )}
                </Field>
                <TimeUnitInput label="Certificate Lifetime"
                               update={(value, unit) => {
                                 setFieldValue('lifetimeValue', value);
                                 setFieldValue('lifetimeUnit', unit);
                               }}
                               value={values.lifetimeValue}
                               unit={values.lifetimeUnit.toLocaleUpperCase()}
                               enabled
                               hideCheckbox
                               units={TIME_UNITS_UPPER} />
              </Modal.Body>
              <Modal.Footer>
                <ModalSubmit onCancel={() => onCancel()}
                             isSubmitting={isSubmitting}
                             isAsyncSubmit
                             submitButtonText="Create Certificate"
                             submitLoadingText="Creating certificate..." />
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
              <Button bsStyle="success" onClick={() => onCancel()}>Close</Button>
            </ButtonToolbar>
          </Modal.Footer>
        </>
      )}
    </>
  );
};

export default ClientCertForm;
