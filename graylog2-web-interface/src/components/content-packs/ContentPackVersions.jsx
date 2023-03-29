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
import PropTypes from 'prop-types';
import React from 'react';

import { LinkContainer } from 'components/common/router';
import Routes from 'routing/Routes';
import { DataTable, ModalSubmit } from 'components/common';
import { BootstrapModalWrapper, Button, DropdownButton, ButtonToolbar, MenuItem, Modal } from 'components/bootstrap';
import ContentPackDownloadControl from 'components/content-packs/ContentPackDownloadControl';
import ContentPackInstall from 'components/content-packs/ContentPackInstall';

import './ContentPackVersions.css';

class ContentPackVersions extends React.Component {
  static propTypes = {
    contentPackRevisions: PropTypes.object.isRequired,
    onChange: PropTypes.func,
    onDeletePack: PropTypes.func,
    onInstall: PropTypes.func,
  };

  static defaultProps = {
    onChange: () => {},
    onDeletePack: () => {},
    onInstall: () => {},
  };

  constructor(props) {
    super(props);
    const { contentPackRevisions } = this.props;

    this.state = {
      showModal: false,
      selectedVersion: contentPackRevisions.latestRevision,
    };

    this.onChange = this.onChange.bind(this);
    this.rowFormatter = this.rowFormatter.bind(this);
    this.headerFormatter = this.headerFormatter.bind(this);
  }

  onChange(event) {
    const { onChange } = this.props;

    this.setState({
      selectedVersion: event.target.value,
    });

    onChange(event.target.value);
  }

  // eslint-disable-next-line class-methods-use-this
  headerFormatter = (header) => {
    if (header === 'Action') {
      return (<th className="text-right">{header}</th>);
    }

    return (<th>{header}</th>);
  };

  rowFormatter(pack) {
    const { onDeletePack } = this.props;
    const { selectedVersion } = this.state;

    const { openFunc, installModal } = this._installModal(pack);
    let downloadRef;
    const downloadModal = (
      <ContentPackDownloadControl ref={(node) => { downloadRef = node; }}
                                  contentPackId={pack.id}
                                  revision={pack.rev} />
    );

    return (
      <tr key={pack.id + pack.rev}>
        <td>
          <input type="radio"
                 value={pack.rev}
                 onChange={this.onChange}
                 checked={parseInt(selectedVersion, 10) === pack.rev} />
        </td>
        <td>{pack.rev}</td>
        <td className="text-right">
          <ButtonToolbar className="pull-right">
            <Button bsStyle="success" bsSize="small" onClick={() => { downloadRef.open(); }}>Download</Button>
            <DropdownButton id={`action-${pack.rev}`} title="Actions" bsSize="small">
              <MenuItem onClick={openFunc}>Install</MenuItem>
              <LinkContainer to={Routes.SYSTEM.CONTENTPACKS.edit(encodeURIComponent(pack.id), encodeURIComponent(pack.rev))}>
                <MenuItem>Create New From Revision</MenuItem>
              </LinkContainer>
              <MenuItem divider />
              <MenuItem onClick={() => { onDeletePack(pack.id, pack.rev); }}>Delete</MenuItem>
              {installModal}
            </DropdownButton>
          </ButtonToolbar>
        </td>
        {downloadModal}
      </tr>
    );
  }

  _installModal(item) {
    let installRef;

    const { onInstall: onInstallProp } = this.props;

    const closeModal = () => {
      this.setState({ showModal: false });
    };

    const open = () => {
      this.setState({ showModal: true });
    };

    const onInstall = () => {
      installRef.onInstall();
      closeModal();
    };

    const modal = (
      <BootstrapModalWrapper showModal={this.state.showModal}
                             onHide={closeModal}
                             bsSize="large"
                             data-app-section="content_pack_versions"
                             data-event-element="Install">
        <Modal.Header closeButton>
          <Modal.Title>Install</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <ContentPackInstall ref={(node) => { installRef = node; }}
                              contentPack={item}
                              onInstall={onInstallProp} />
        </Modal.Body>
        <Modal.Footer>
          <ModalSubmit onSubmit={onInstall}
                       submitButtonType="button"
                       onCancel={closeModal}
                       submitButtonText="Install" />
        </Modal.Footer>
      </BootstrapModalWrapper>
    );

    return { openFunc: open, installModal: modal };
  }

  render() {
    const { contentPackRevisions: { contentPacks } } = this.props;
    const headers = ['Select', 'Revision', 'Action'];

    return (
      <DataTable id="content-packs-versions"
                 headers={headers}
                 headerCellFormatter={this.headerFormatter}
                 useNumericSort
                 sortBy={(c) => c.rev.toString()}
                 dataRowFormatter={this.rowFormatter}
                 rows={contentPacks}
                 filterKeys={[]} />
    );
  }
}

export default ContentPackVersions;
