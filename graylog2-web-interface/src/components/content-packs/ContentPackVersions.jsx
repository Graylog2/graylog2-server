import PropTypes from 'prop-types';
import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';

import Routes from 'routing/Routes';
import { DataTable } from 'components/common';
import { Button, DropdownButton, ButtonToolbar, MenuItem, Modal } from 'components/graylog';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
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
    this.state = { selectedVersion: contentPackRevisions.latestRevision };

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
            <DropdownButton id={`action-${pack.rev}`} bsStyle="info" title="Actions" bsSize="small">
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
    let modalRef;
    let installRef;

    const { onInstall: onInstallProp } = this.props;

    const closeModal = () => {
      modalRef.close();
    };

    const open = () => {
      modalRef.open();
    };

    const onInstall = () => {
      installRef.onInstall();
      modalRef.close();
    };

    const modal = (
      <BootstrapModalWrapper ref={(node) => { modalRef = node; }} bsSize="large">
        <Modal.Header closeButton>
          <Modal.Title>Install</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <ContentPackInstall ref={(node) => { installRef = node; }}
                              contentPack={item}
                              onInstall={onInstallProp} />
        </Modal.Body>
        <Modal.Footer>
          <div className="pull-right">
            <ButtonToolbar>
              <Button bsStyle="primary" onClick={onInstall}>Install</Button>
              <Button onClick={closeModal}>Close</Button>
            </ButtonToolbar>
          </div>
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
                 sortBy={(c) => c.rev.toString()}
                 dataRowFormatter={this.rowFormatter}
                 rows={contentPacks}
                 filterKeys={[]} />
    );
  }
}

export default ContentPackVersions;
