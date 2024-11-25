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
import React from 'react';

import { DataTable } from 'components/common';
import { BootstrapModalWrapper, Button, ButtonToolbar, Modal } from 'components/bootstrap';
import Spinner from 'components/common/Spinner';
import ContentPackInstallationView from 'components/content-packs/ContentPackInstallView';

type ContentPackInstallationsProps = {
  installations?: any[];
  onUninstall?: (...args: any[]) => void;
};

class ContentPackInstallations extends React.Component<ContentPackInstallationsProps, {
  [key: string]: any;
}> {
  static defaultProps = {
    installations: [],
    onUninstall: () => {
    },
  };

  constructor(props) {
    super(props);

    this.state = {
      showInstallModal: false,
    };
  }

  rowFormatter = (item) => {
    const { onUninstall } = this.props;

    const closeShowModal = () => {
      this.setState({ showInstallModal: false });
    };

    const openShowModal = () => {
      this.setState({ showInstallModal: true });
    };

    const installModal = (
      <BootstrapModalWrapper showModal={this.state.showInstallModal}
                             onHide={closeShowModal}
                             bsSize="large">
        <Modal.Header closeButton>
          <Modal.Title>View Installation</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <ContentPackInstallationView install={item} />
        </Modal.Body>
        <Modal.Footer>
          <Button onClick={closeShowModal}>Close</Button>
        </Modal.Footer>
      </BootstrapModalWrapper>
    );

    return (
      <tr key={item}>
        <td>
          {item.comment}
        </td>
        <td>{item.content_pack_revision}</td>
        <td>
          <div className="pull-right">
            <ButtonToolbar>
              <Button bsStyle="primary"
                      bsSize="small"
                      onClick={() => {
                        onUninstall(item.content_pack_id, item._id);
                      }}>
                Uninstall
              </Button>
              <Button bsStyle="info"
                      bsSize="small"
                      onClick={openShowModal}>
                View
              </Button>
              {installModal}
            </ButtonToolbar>
          </div>
        </td>
      </tr>
    );
  };

  // eslint-disable-next-line class-methods-use-this
  headerFormater = (header) => {
    if (header === 'Action') {
      return (<th className="text-right">{header}</th>);
    }

    return (<th>{header}</th>);
  };

  render() {
    const { installations } = this.props;

    if (!installations) {
      return (<Spinner />);
    }

    const headers = ['Comment', 'Version', 'Action'];

    return (
      <DataTable id="content-packs-versions"
                 headers={headers}
                 headerCellFormatter={this.headerFormater}
                 useNumericSort
                 sortBy={(c) => c.content_pack_revision.toString()}
                 dataRowFormatter={this.rowFormatter}
                 rows={installations}
                 filterKeys={[]} />
    );
  }
}

export default ContentPackInstallations;
