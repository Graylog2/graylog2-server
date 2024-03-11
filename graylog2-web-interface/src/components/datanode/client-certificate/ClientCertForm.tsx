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
import { Formik, Form } from 'formik';
import { useState } from 'react';

import { FormikInput } from 'components/common';
import { Button, ButtonToolbar, Modal } from 'components/bootstrap';
import type { ClientCertFormValues } from 'components/datanode/hooks/useCreateDataNodeClientCert';
import useCreateDataNodeClientCert from 'components/datanode/hooks/useCreateDataNodeClientCert';
import ClientCertificateView from 'components/datanode/client-certificate/ClientCertificateView';

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
        <Formik initialValues={{ principal: '', role: 'all_access', password: '' }} onSubmit={(formValues: ClientCertFormValues) => onSubmit(formValues)}>
          {({ isSubmitting }) => (
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
                             label="Password"
                             required />
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
