import React from 'react';

import { Alert } from 'components/bootstrap';
import { DataTable, ExternalLink, Spinner } from 'components/common';

type InputTypesDataTableProps = {
  inputDescriptions?: any;
};

class InputTypesDataTable extends React.Component<InputTypesDataTableProps, {
  [key: string]: any;
}> {
  _headerCellFormatter = (header) => <th>{header}</th>;

  _inputTypeFormatter = (inputType) => (
    <tr key={inputType.type}>
      <td className="limited">{inputType.name}</td>
      <td className="limited">{inputType.type}</td>
      <td className="limited" style={{ width: 150 }}>
        {inputType.link_to_docs
          && <ExternalLink href={inputType.link_to_docs}>Documentation</ExternalLink>}
      </td>
    </tr>
  );

  render() {
    if (!this.props.inputDescriptions) {
      return <Spinner text="Loading input types..." />;
    }

    if (Object.keys(this.props.inputDescriptions).length === 0) {
      return (
        <Alert bsStyle="warning">
          Input types are unavailable.
        </Alert>
      );
    }

    const headers = ['Name', 'Type', 'Documentation'];

    const rows = Object.keys(this.props.inputDescriptions).map((key) => this.props.inputDescriptions[key]);

    return (
      <DataTable id="input-types-list"
                 rowClassName="row-sm"
                 className="table-hover table-condensed table-striped"
                 headers={headers}
                 headerCellFormatter={this._headerCellFormatter}
                 sortByKey="name"
                 rows={rows}
                 dataRowFormatter={this._inputTypeFormatter}
                 filterLabel="Filter"
                 filterKeys={[]} />
    );
  }
}

export default InputTypesDataTable;