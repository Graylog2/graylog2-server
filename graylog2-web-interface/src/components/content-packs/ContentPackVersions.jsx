import PropTypes from 'prop-types';
import React from 'react';

import { DataTable } from 'components/common';
import { Button, DropdownButton, MenuItem } from 'react-bootstrap';
import ContentPackDownloadControl from 'components/content-packs/ContentPackDownloadControl';

import './ContentPackVersions.css';

class ContentPackVersions extends React.Component {
  static propTypes = {
    contentPack: PropTypes.object.isRequired,
    onChange: PropTypes.func,
  };

  static defaultProps = {
    onChange: () => {},
  };

  constructor(props) {
    super(props);
    const versions = Object.keys(this.props.contentPack);
    this.state = { selectedVersion: versions[0] };

    this.onChange = this.onChange.bind(this);
    this.rowFormatter = this.rowFormatter.bind(this);
    this.headerFormater = this.headerFormater.bind(this);
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
          <Button bsStyle="info" bsSize="small">View</Button>
          &nbsp;
          <DropdownButton id={`more-actions-${pack.id + pack.rev}`} title="More Actions" bsSize="small" pullRight>
            <MenuItem>Remove</MenuItem>
            <MenuItem onSelect={() => { downloadRef.open(); }}>Download</MenuItem>
          </DropdownButton>
        </td>
        {downloadModal}
      </tr>
    );
  }

  headerFormater(header) {
    if (header === 'Action') {
      return (<th className="text-right">{header}</th>);
    } else {
      return (<th>{header}</th>);
    }
  }

  render() {
    const versions = Object.keys(this.props.contentPack).map((rev) => { return { version: rev }; });
    const headers = ['Select', 'Revision', 'Action'];
    return (
      <DataTable
        id="content-packs-versions"
        headers={headers}
        headerCellFormatter={this.headerFormater}
        sortByKey="version"
        dataRowFormatter={this.rowFormatter}
        rows={versions}
        filterKeys={[]}
      />);
  }
}

export default ContentPackVersions;
