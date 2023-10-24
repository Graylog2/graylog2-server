import * as React from 'react';
import { useState, useRef } from 'react';
import PropTypes from 'prop-types';

import {
  Button,
  Col,
  DropdownButton,
  MenuItem,
  Modal,
  Row,
  ButtonToolbar,
} from 'components/bootstrap';
import { ModalSubmit } from 'components/common';
import ControlledTableListItem from 'components/common/ControlledTableListItem';
import { LinkContainer, Link } from 'components/common/router';
import ContentPackStatus from 'components/content-packs/ContentPackStatus';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
import ContentPackInstall from 'components/content-packs/ContentPackInstall';
import ContentPackDownloadControl from 'components/content-packs/ContentPackDownloadControl';
import Routes from 'routing/Routes';

type Props = {
  pack: any,
  contentPackMetadata: any,
  onDeletePack: (id: string) => void,
  onInstall: () => void,
};

const ContentPackListItem = ({ pack, contentPackMetadata, onDeletePack, onInstall }: Props) => {
  const [showInstallModal, setShowInstallModal] = useState(false);
  const [showDownloadModal, setShowDownloadModal] = useState(false);

  const metadata = contentPackMetadata[pack.id] || {};
  const installed = Object.keys(metadata).find((rev) => metadata[rev].installation_count > 0);
  const states = installed ? ['installed'] : [];
  const updateButton = states.includes('updatable') ? <Button bsSize="small" bsStyle="primary">Update</Button> : '';

  const handleInstall = () => setShowInstallModal(true);

  const handleDownload = () => setShowDownloadModal(true);

  const handleDeleteAllVersions = () => onDeletePack(pack.id);

  const onCloseInstallModal = () => setShowInstallModal(false);

  return (
    <ControlledTableListItem>
      <Row className="row-sm">
        <Col md={9}>
          <h3><Link to={Routes.SYSTEM.CONTENTPACKS.show(pack.id)}>{pack.name}</Link>
            <small>Latest
              Version: {pack.rev} <ContentPackStatus contentPackId={pack.id} states={states} />
            </small>
          </h3>
        </Col>
        <Col md={3} className="text-right">
          <ButtonToolbar className="pull-right">
            {updateButton}
            <Button bsSize="small" onClick={handleInstall}>Install</Button>
            <DropdownButton id={`more-actions-${pack.id}`} title="More Actions" bsSize="small" pullRight>
              <LinkContainer to={Routes.SYSTEM.CONTENTPACKS.show(pack.id)}>
                <MenuItem>Show</MenuItem>
              </LinkContainer>
              <LinkContainer to={Routes.SYSTEM.CONTENTPACKS.edit(encodeURIComponent(pack.id), encodeURIComponent(pack.rev))}>
                <MenuItem>Create New Version</MenuItem>
              </LinkContainer>
              <MenuItem onSelect={handleDownload}>
                Download
              </MenuItem>
              <MenuItem divider />
              <MenuItem onSelect={handleDeleteAllVersions}>
                Delete All Versions
              </MenuItem>
            </DropdownButton>
          </ButtonToolbar>
        </Col>
      </Row>
      <Row className="row-sm content-packs-summary">
        <Col md={12}>
          {pack.summary}&nbsp;
        </Col>
      </Row>
      {showInstallModal && (
        <BootstrapModalWrapper showModal={showInstallModal}
                               onHide={onCloseInstallModal}
                               bsSize="large">
          <Modal.Header closeButton>
            <Modal.Title>Install Content Pack</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <ContentPackInstall contentPack={pack}
                                onInstall={onInstall} />
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
    </ControlledTableListItem>

  );
};

ContentPackListItem.propTypes = {
  pack: PropTypes.object.isRequired,
  contentPackMetadata: PropTypes.object.isRequired,
  onDeletePack: PropTypes.func.isRequired,
  onInstall: PropTypes.func.isRequired,
};

ContentPackListItem.defaultProps = {

};

export default ContentPackListItem;
