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
import { useState } from 'react';

import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import { Modal, Button } from 'components/bootstrap';
import { Icon } from 'components/common';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';

type Props = {
  contentPackId: string,
  revision: number,
  show?: boolean
  onHide?: () => void
}

const ContentPackDownloadControl = ({
  contentPackId, revision, show = false, onHide = () => {
  },
}: Props) => {
  const [showDownloadModal, setShowDownloadModal] = useState(show);

  const getDownloadUrl = () => qualifyUrl(ApiRoutes.ContentPacksController.downloadRev(contentPackId, revision).url);

  const closeModal = () => {
    setShowDownloadModal(false);
    onHide();
  };

  const infoText = 'Please right click the download link below and choose "Save Link As..." to download the JSON file.';
  const modalTitle = 'Download Content Pack';

  return (
    <BootstrapModalWrapper showModal={showDownloadModal}
                           onHide={closeModal}
                           bsSize="large">
      <Modal.Header closeButton>
        <Modal.Title>{modalTitle}</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <p>{infoText}</p>
        <p>
          <a href={getDownloadUrl()} target="_blank" rel="noopener noreferrer">
            <Icon name="download" />{' '}Download
          </a>
        </p>
      </Modal.Body>
      <Modal.Footer>
        <Button onClick={closeModal}>Close</Button>
      </Modal.Footer>
    </BootstrapModalWrapper>
  );
};

export default ContentPackDownloadControl;
