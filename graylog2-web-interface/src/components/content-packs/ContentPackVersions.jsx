import PropTypes from 'prop-types';
import React from 'react';

import Routes from 'routing/Routes';
import { DataTable } from 'components/common';
import { Button, DropdownButton, ButtonToolbar, MenuItem, Modal } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';

import ContentPackDownloadControl from 'components/content-packs/ContentPackDownloadControl';
import ContentPackInstall from 'components/content-packs/ContentPackInstall';

import './ContentPackVersions.css';

class ContentPackVersions extends React.Component {
  static propTypes = {
    contentPack: PropTypes.object.isRequired,
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
    const versions = Object.keys(this.props.contentPack);
    this.state = { selectedVersion: versions[0] };

    this.onChange = this.onChange.bind(this);
    this.rowFormatter = this.rowFormatter.bind(this);
    this.headerFormatter = this.headerFormatter.bind(this);
  }

  onChange(event) {
    this.setState({
      selectedVersion: event.target.value,
    });
    this.props.onChange(event.target.value);
  }

  _installModal(item) {
    let modalRef;
    let installRef;

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
                              onInstall={this.props.onInstall} />
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

  rowFormatter(rev) {
    const pack = this.props.contentPack[parseInt(rev.version, 10)];
    const { openFunc, installModal } = this._installModal(pack);
    let downloadRef;
    const downloadModal = (<ContentPackDownloadControl
      ref={(node) => { downloadRef = node; }}
      contentPackId={pack.id}
      revision={pack.rev}
    />);
    return (
      <tr key={pack.id + pack.rev}>
        <td>
          <input type="radio" value={pack.rev} onChange={this.onChange} checked={this.state.selectedVersion === pack.rev.toString()} />
        </td>
        <td>{pack.rev}</td>
        <td className="text-right">
          <ButtonToolbar className="pull-right">
            <Button bsStyle="success" bsSize="small" onClick={() => { downloadRef.open(); }}>Download</Button>
            <DropdownButton id={`action-${pack.rev}`} bsStyle="info" title="Actions" bsSize="small">
              <LinkContainer to={Routes.SYSTEM.CONTENTPACKS.edit(encodeURIComponent(pack.id), encodeURIComponent(pack.rev))}>
                <MenuItem >Edit clone</MenuItem>
              </LinkContainer>
              <MenuItem onClick={() => { this.props.onDeletePack(pack.id, pack.rev); }}>Delete</MenuItem>
              <MenuItem onClick={openFunc}>Install</MenuItem>
              {installModal}
            </DropdownButton>
          </ButtonToolbar>
        </td>
        {downloadModal}
      </tr>
    );
  }

  headerFormatter = (header) => {
    if (header === 'Action') {
      return (<th className="text-right">{header}</th>);
    }
    return (<th>{header}</th>);
  };

  render() {
    const versions = Object.keys(this.props.contentPack).map((rev) => { return { version: rev }; });
    const headers = ['Select', 'Revision', 'Action'];
    return (
      <DataTable
        id="content-packs-versions"
        headers={headers}
        headerCellFormatter={this.headerFormatter}
        sortByKey="version"
        dataRowFormatter={this.rowFormatter}
        rows={versions}
        filterKeys={[]}
      />);
  }
}

export default ContentPackVersions;
