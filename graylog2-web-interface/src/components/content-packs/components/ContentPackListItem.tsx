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
import { useTranslation } from 'react-i18next';

import { Button, Col, DropdownButton, MenuItem, Row, ButtonToolbar, DeleteMenuItem, Modal } from 'components/bootstrap';
import { ModalSubmit, LinkContainer, Link } from 'components/common';
import ControlledTableListItem from 'components/common/ControlledTableListItem';
import ContentPackStatus from 'components/content-packs/ContentPackStatus';
import ContentPackInstall from 'components/content-packs/ContentPackInstall';
import ContentPackDownloadControl from 'components/content-packs/ContentPackDownloadControl';
import Routes from 'routing/Routes';
import type { EntitySharePayload } from 'actions/permissions/EntityShareActions';

import type { ContentPackInstallation, ContentPackMetadata } from '../Types';

type Props = {
  pack: ContentPackInstallation;
  contentPackMetadata: ContentPackMetadata;
  onDeletePack: (id: string) => void;
  onInstall: (id: string, contentPackRev: number, parameters: unknown, shareRequest: EntitySharePayload) => void;
};

const ContentPackListItem = ({ pack, contentPackMetadata, onDeletePack, onInstall: onInstallProp }: Props) => {
  const { t } = useTranslation('content_packs');
  const [showInstallModal, setShowInstallModal] = useState(false);
  const [showDownloadModal, setShowDownloadModal] = useState(false);
  const installRef = useRef(null);
  const metadata = contentPackMetadata[pack.id] || {};
  const installed = Object.keys(metadata).find((rev) => metadata[rev].installation_count > 0);
  const states = installed ? ['installed'] : [];
  const updateButton = states.includes('updatable') ? (
    <Button bsSize="small" bsStyle="primary">
      {t('list_item.update')}
    </Button>
  ) : (
    ''
  );

  const handleInstall = () => setShowInstallModal(true);

  const handleDownload = () => setShowDownloadModal(true);

  const handleDeleteAllVersions = () => onDeletePack(pack.id);

  const onCloseInstallModal = () => setShowInstallModal(false);

  const onInstall = () => {
    if (installRef.current !== null) {
      installRef.current?.onInstall();
    }

    setShowInstallModal(false);
  };

  return (
    <ControlledTableListItem>
      <Row className="row-sm">
        <Col md={9}>
          <h3>
            <Link to={Routes.SYSTEM.CONTENTPACKS.show(pack.id)}>{pack.name}</Link>{' '}
            <small>
              {t('list_item.latest_version', { rev: pack.rev })}{' '}
              <ContentPackStatus contentPackId={pack.id} states={states} />
            </small>
          </h3>
        </Col>
        <Col md={3} className="text-right">
          <ButtonToolbar className="pull-right">
            {updateButton}
            <Button bsSize="small" onClick={handleInstall}>
              {t('list_item.install')}
            </Button>
            <DropdownButton
              id={`more-actions-${pack.id}`}
              title={t('common:actions.more_actions')}
              bsSize="small"
              pullRight>
              <LinkContainer to={Routes.SYSTEM.CONTENTPACKS.show(pack.id)}>
                <MenuItem>{t('list_item.show')}</MenuItem>
              </LinkContainer>
              <LinkContainer
                to={Routes.SYSTEM.CONTENTPACKS.edit(encodeURIComponent(pack.id), encodeURIComponent(pack.rev))}>
                <MenuItem>{t('list_item.create_new_version')}</MenuItem>
              </LinkContainer>
              <MenuItem onSelect={handleDownload}>{t('download.download')}</MenuItem>
              <MenuItem divider />
              <DeleteMenuItem onSelect={handleDeleteAllVersions}>{t('list_item.delete_all_versions')}</DeleteMenuItem>
            </DropdownButton>
          </ButtonToolbar>
        </Col>
      </Row>
      <Row className="row-sm content-packs-summary">
        <Col md={12}>{pack.summary}&nbsp;</Col>
      </Row>
      {showInstallModal && (
        <Modal show={showInstallModal} onHide={onCloseInstallModal} bsSize="large">
          <Modal.Header>
            <Modal.Title>{t('list_item.install_title')}</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <ContentPackInstall ref={installRef} contentPack={pack} onInstall={onInstallProp} />
          </Modal.Body>
          <Modal.Footer>
            <ModalSubmit
              submitButtonText={t('list_item.install')}
              onSubmit={onInstall}
              onCancel={onCloseInstallModal}
            />
          </Modal.Footer>
        </Modal>
      )}
      {showDownloadModal && (
        <ContentPackDownloadControl
          show={showDownloadModal}
          onHide={() => setShowDownloadModal(false)}
          contentPackId={pack.id}
          revision={pack.rev}
        />
      )}
    </ControlledTableListItem>
  );
};

export default ContentPackListItem;
