import PropTypes from 'prop-types';
import React from 'react';

import Routes from 'routing/Routes';
import { DataTable } from 'components/common';
import { Button, DropdownButton, ButtonToolbar, MenuItem } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import ContentPackDownloadControl from 'components/content-packs/ContentPackDownloadControl';

import './ContentPackVersions.css';

class ContentPackVersions extends React.Component {
  static propTypes = {
    contentPack: PropTypes.object.isRequired,
    onChange: PropTypes.func,
    onDeletePack: PropTypes.func,
  };

  static defaultProps = {
    onChange: () => {},
    onDeletePack: () => {},
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

  rowFormatter(rev) {
    const pack = this.props.contentPack[parseInt(rev.version, 10)];
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
