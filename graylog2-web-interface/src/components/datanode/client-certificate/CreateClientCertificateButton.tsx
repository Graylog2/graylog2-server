import React, { useState } from 'react';

import { BootstrapModalWrapper, Button } from 'components/bootstrap';
import ClientCertForm from 'components/datanode/client-certificate/ClientCertForm';

const CreateClientCertificateButton = () => {
  const [showCertificateForm, setShowCertificateForm] = useState(false);
  const onCancel = () => setShowCertificateForm(false);

  return (
    <>
      <Button bsStyle="info" bsSize="xs" onClick={() => setShowCertificateForm(true)}>
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
