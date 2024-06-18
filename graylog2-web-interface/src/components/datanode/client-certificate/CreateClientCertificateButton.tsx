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
import React, { useState } from 'react';

import { BootstrapModalWrapper, Button } from 'components/bootstrap';
import ClientCertForm from 'components/datanode/client-certificate/ClientCertForm';

const CreateClientCertificateButton = () => {
  const [showCertificateForm, setShowCertificateForm] = useState(false);
  const onCancel = () => setShowCertificateForm(false);

  return (
    <>
      <Button bsStyle="primary" bsSize="small" onClick={() => setShowCertificateForm(true)}>
        Generate client certificate
      </Button>
      {showCertificateForm && (
        <BootstrapModalWrapper showModal={showCertificateForm}
                               onHide={() => onCancel()}
                               bsSize="lg">

          <ClientCertForm onCancel={onCancel} />
        </BootstrapModalWrapper>
      )}
    </>
  );
};

export default CreateClientCertificateButton;
