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
import PropTypes from 'prop-types';
import { Alert, Button, Modal } from 'components/bootstrap';

import Agree from './Agree';
import KinesisSetupSteps from './KinesisSetupSteps';

const SetupModal = ({ onSubmit, onCancel, groupName, streamName }) => {
  const [agreed, setAgreed] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState(false);

  const buttonOtherText = (!error && !success) ? 'Creating...' : 'Close';
  const buttonText = success ? 'Continue Setup' : buttonOtherText;

  const handleSuccess = () => {
    setSuccess(true);
    setError(false);
  };

  const handleError = () => {
    setSuccess(false);
    setError(true);
  };

  return (
    <Modal show>
      <Modal.Header>
        <Modal.Title>{agreed ? 'Executing Auto-Setup' : 'Kinesis Auto Setup Agreement'}</Modal.Title>
      </Modal.Header>

      <Modal.Body>
        {agreed
          ? <KinesisSetupSteps onSuccess={handleSuccess} onError={handleError} />
          : <Agree groupName={groupName} streamName={streamName} />}

        {agreed && success && (
          <Alert key="delayedLogs" variant="warning">
            It may take up to ten minutes for the first messages to arrive in the Kinesis stream. The Kinesis Health Check in the following step will not complete successfully until messages are present in the stream. Please see the official <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/Subscriptions.html" target="_blank" rel="noopener noreferrer">CloudWatch Subscriptions</a> documentation for more information.
          </Alert>
        )}
      </Modal.Body>

      <Modal.Footer>
        {agreed
          ? (
            <Button bsStyle="success"
                    onClick={success ? onSubmit : onCancel}
                    type="button"
                    disabled={!error && !success}>
              {buttonText}
            </Button>
          )
          : (
            <>
              <Button onClick={onCancel}
                      type="button">
                Cancel
              </Button>
              <Button onClick={() => (setAgreed(true))}
                      type="button"
                      bsStyle="success">
                I Agree! Create these AWS resources now.
              </Button>
            </>
          )}
      </Modal.Footer>
    </Modal>
  );
};

SetupModal.propTypes = {
  onSubmit: PropTypes.func.isRequired,
  onCancel: PropTypes.func.isRequired,
  groupName: PropTypes.string.isRequired,
  streamName: PropTypes.string.isRequired,
};

export default SetupModal;
