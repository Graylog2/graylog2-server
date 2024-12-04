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
import { useState, useRef } from 'react';

import { LinkContainer } from 'components/common/router';
import Routes from 'routing/Routes';
import { ModalSubmit } from 'components/common';
import ContentPackDownloadControl from 'components/content-packs/ContentPackDownloadControl';
import ContentPackInstall from 'components/content-packs/ContentPackInstall';
import {
  BootstrapModalWrapper,
  Button,
  DropdownButton,
  ButtonToolbar,
  MenuItem,
  Modal,
  DeleteMenuItem,
} from 'components/bootstrap';
import type { ContentPackInstallation } from 'components/content-packs/Types';
import type ContentPackRevisions from 'logic/content-packs/ContentPackRevisions';

type Props = {
  pack: ContentPackInstallation
  contentPackRevisions: ContentPackRevisions,
  onDeletePack?: (id: string, rev: number) => void
  onChange?: (id: string) => void
  onInstall?: (id: string, contentPackRev: string, parameters: unknown) => void
};

const ContentPackVersionItem = ({
  pack,
  contentPackRevisions,
  onChange: onChangeProp = () => {},
  onDeletePack = () => {},
  onInstall: onInstallProp = () => {},
}: Props) => {
  const [showInstallModal, setShowInstallModal] = useState(false);
  const [showDownloadModal, setShowDownloadModal] = useState(false);
  const [selectedVersion, setSelectedVersion] = useState(contentPackRevisions.latestRevision);
  const installRef = useRef(null);

  const handleInstall = () => setShowInstallModal(true);

  const handleDownload = () => setShowDownloadModal(true);

  const onCloseInstallModal = () => setShowInstallModal(false);

  const onChange = (event) => {
    const version = event.target.value;

    setSelectedVersion(version);
    onChangeProp(version);
  };

  const onInstall = () => {
    if (installRef.current !== null) {
      installRef.current?.onInstall();
    }

    setShowInstallModal(false);
  };

  return (
    <tr key={pack.id + pack.rev}>
      <td>
        <input type="radio"
               value={pack.rev}
               onChange={onChange}
               checked={selectedVersion === pack.rev} />
      </td>
      <td>{pack.rev}</td>
      <td className="text-right">
        <ButtonToolbar className="pull-right">
          <Button bsStyle="success"
                  bsSize="small"
                  onClick={() => handleDownload()}>
            Download
          </Button>
          <DropdownButton id={`action-${pack.rev}`} title="Actions" bsSize="small">
            <MenuItem onClick={() => handleInstall()}>Install</MenuItem>
            <LinkContainer to={Routes.SYSTEM.CONTENTPACKS.edit(encodeURIComponent(pack.id), encodeURIComponent(pack.rev))}>
              <MenuItem>Create New From Revision</MenuItem>
            </LinkContainer>
            <MenuItem divider />
            <DeleteMenuItem onClick={() => { onDeletePack(pack.id, pack.rev); }} />
          </DropdownButton>
        </ButtonToolbar>
      </td>
      {showInstallModal && (
        <BootstrapModalWrapper showModal={showInstallModal}
                               onHide={onCloseInstallModal}
                               bsSize="large">
          <Modal.Header closeButton>
            <Modal.Title>Install Content Pack</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <ContentPackInstall ref={installRef}
                                contentPack={pack}
                                onInstall={onInstallProp} />
          </Modal.Body>
          <Modal.Footer>
            <ModalSubmit submitButtonText="Install" onSubmit={onInstall} onCancel={onCloseInstallModal} />
          </Modal.Footer>
        </BootstrapModalWrapper>
      )}
      {showDownloadModal && (
        <ContentPackDownloadControl show={showDownloadModal}
                                    onHide={() => setShowDownloadModal(false)}
                                    contentPackId={pack.id}
                                    revision={pack.rev} />
      )}

    </tr>
  );
};

export default ContentPackVersionItem;
