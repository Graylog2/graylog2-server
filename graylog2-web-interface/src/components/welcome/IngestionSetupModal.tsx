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
import { useTranslation } from 'react-i18next';

import { BootstrapModalWrapper, Modal, Button } from 'components/bootstrap';

type Props = {
  show: boolean;
  onHide: () => void;
};

// Placeholder for the upcoming guided first-use ingestion setup wizard.
const IngestionSetupModal = ({ show, onHide }: Props) => {
  const { t } = useTranslation('welcome');

  return (
    <BootstrapModalWrapper showModal={show} onHide={onHide}>
      <Modal.Header>
        <Modal.Title>{t('onboarding.ingestion_title')}</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <p>{t('onboarding.ingestion_coming_soon')}</p>
      </Modal.Body>
      <Modal.Footer>
        <Button onClick={onHide}>{t('content_stream.close')}</Button>
      </Modal.Footer>
    </BootstrapModalWrapper>
  );
};

export default IngestionSetupModal;
