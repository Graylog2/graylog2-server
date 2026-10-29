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
import React, { useState, useCallback } from 'react';
import { Trans, useTranslation } from 'react-i18next';

import { Alert, Button, Modal } from 'components/bootstrap';
import { ModalSubmit } from 'components/common';

import Agree from './Agree';
import KinesisSetupSteps from './KinesisSetupSteps';

type SetupModalProps = {
  onSubmit: (...args: any[]) => void;
  onCancel: (...args: any[]) => void;
  groupName: string;
  streamName: string;
};

const DocsAnchor = ({ href, children }: { href: string; children?: React.ReactNode }) => (
  <a href={href} target="_blank" rel="noopener noreferrer">
    {children}
  </a>
);

const SetupModal = ({ onSubmit, onCancel, groupName, streamName }: SetupModalProps) => {
  const { t } = useTranslation('integrations');
  const [agreed, setAgreed] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState(false);

  const buttonOtherText = !error && !success ? t('aws_kinesis_setup_modal.creating_button') : t('aws_kinesis_setup_modal.close_button');
  const buttonText = success ? t('aws_kinesis_setup_modal.continue_button') : buttonOtherText;

  const handleSuccess = () => {
    setSuccess(true);
    setError(false);
  };

  const handleError = useCallback(() => {
    setSuccess(false);
    setError(true);
  }, []);

  return (
    <Modal show onHide={() => {}}>
      <Modal.Header>
        <Modal.Title>
          {agreed ? t('aws_kinesis_setup_modal.executing_title') : t('aws_kinesis_setup_modal.agreement_title')}
        </Modal.Title>
      </Modal.Header>

      <Modal.Body>
        {agreed ? (
          <KinesisSetupSteps onSuccess={handleSuccess} onError={handleError} />
        ) : (
          <Agree groupName={groupName} streamName={streamName} />
        )}

        {agreed && success && (
          <Alert key="delayedLogs" bsStyle="warning">
            <Trans
              t={t}
              i18nKey="aws_kinesis_setup_modal.delayed_logs"
              components={{
                cloudWatchLink: (
                  <DocsAnchor href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/Subscriptions.html" />
                ),
              }}
            />
          </Alert>
        )}
      </Modal.Body>

      <Modal.Footer>
        {agreed ? (
          <Button bsStyle="primary" onClick={success ? onSubmit : onCancel} type="button" disabled={!error && !success}>
            {buttonText}
          </Button>
        ) : (
          <ModalSubmit
            submitButtonText={t('aws_kinesis_setup_modal.agree_button')}
            onSubmit={() => setAgreed(true)}
            submitButtonType="button"
            onCancel={onCancel}
          />
        )}
      </Modal.Footer>
    </Modal>
  );
};

export default SetupModal;
