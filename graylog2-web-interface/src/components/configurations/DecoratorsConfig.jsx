// @flow strict
import React, { useCallback, useState, useRef } from 'react';
import PropTypes from 'prop-types';

import { IfPermitted } from 'components/common';
import { Button } from 'components/graylog';
import { BootstrapModalForm } from 'components/bootstrap';

const DecoratorsConfig = () => {
  const configModal = useRef();
  const openModal = useCallback(() => configModal.current.open(), [configModal]);
  const closeModal = useCallback(() => configModal.current.close(), [configModal]);
  const saveConfig = () => {};
  return (
    <div>
      <h2>Decorators Configuration</h2>
      <IfPermitted permissions="clusterconfigentry:edit">
        <Button bsStyle="info" bsSize="xs" onClick={openModal}>Update</Button>
      </IfPermitted>
      <BootstrapModalForm ref={configModal}
                          title="Update Search Configuration"
                          onSubmitForm={saveConfig}
                          onModalClose={closeModal}
                          submitButtonText="Save">
        <fieldset>
        </fieldset>
      </BootstrapModalForm>
    </div>
  );
};

DecoratorsConfig.propTypes = {};

export default DecoratorsConfig;
