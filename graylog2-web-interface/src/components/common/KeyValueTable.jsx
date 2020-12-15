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

import { Button } from 'components/graylog';
import { Input } from 'components/bootstrap';
import ObjectUtils from 'util/ObjectUtils';

// eslint-disable-next-line no-unused-vars
import style from './KeyValueTable.css';

/**
 * KeyValueTable displays a table for all key-value pairs in a JS object. If the editable prop is set to true, it also
 * provides inputs to create, edit and delete key-value pairs.
 */
class KeyValueTable extends React.Component {
  static propTypes = {
    /** Object containing key-values to represent in the table. */
    pairs: PropTypes.object.isRequired,
    /** Table headers. Must be an array with three elements [ key header, value header, actions header]. */
    headers: PropTypes.array,
    /** Indicates if the user can create, edit or delete key-value pairs. */
    editable: PropTypes.bool,
    /** Callback when key-value pairs change. It receives the new key-value pairs as argument. */
    onChange: PropTypes.func,
    /** Extra CSS classes for the rendered table. */
    className: PropTypes.string,
    /** Extra CSS classes for the table container. */
    containerClassName: PropTypes.string,
    /** Size of action buttons. */
    actionsSize: PropTypes.oneOf(['large', 'medium', 'small', 'xsmall']),
  };

  static defaultProps = {
    headers: ['Name', 'Value', 'Actions'],
    editable: false,
    actionsSize: 'xsmall',
    className: '',
    containerClassName: '',
  };

  state = {
    newKey: '',
    newValue: '',
  };

  _onPairsChange = (newPairs) => {
    if (this.props.onChange) {
      this.props.onChange(newPairs);
    }
  };

  _bindValue = (event) => {
    const newState = {};

    newState[event.target.name] = event.target.value;
    this.setState(newState);
  };

  _addRow = () => {
    const newPairs = ObjectUtils.clone(this.props.pairs);

    newPairs[this.state.newKey] = this.state.newValue;
    this._onPairsChange(newPairs);

    this.setState({ newKey: '', newValue: '' });
  };

  _deleteRow = (key) => {
    return () => {
      if (window.confirm(`Are you sure you want to delete property '${key}'?`)) {
        const newPairs = ObjectUtils.clone(this.props.pairs);

        delete newPairs[key];
        this._onPairsChange(newPairs);
      }
    };
  };

  _formattedHeaders = (headers) => {
    return (
      <tr>
        {headers.map((header, idx) => {
          const style = {};

          // Hide last column or apply width so it sticks to the right
          if (idx === headers.length - 1) {
            if (!this.props.editable) {
              return null;
            }

            style.width = 75;
          }

          return <th key={header} style={style}>{header}</th>;
        })}
      </tr>
    );
  };

  _formattedRows = (pairs) => {
    return Object.keys(pairs).sort().map((key) => {
      let actionsColumn;

      if (this.props.editable) {
        const actions = [];

        actions.push(
          <Button key={`delete-${key}`} bsStyle="danger" bsSize={this.props.actionsSize} onClick={this._deleteRow(key)}>
            Delete
          </Button>,
        );

        actionsColumn = <td>{actions}</td>;
      }

      return (
        <tr key={key}>
          <td>{key}</td>
          <td>{pairs[key]}</td>
          {actionsColumn}
        </tr>
      );
    });
  };

  _newRow = () => {
    if (!this.props.editable) {
      return null;
    }

    const addRowDisabled = !this.state.newKey || !this.state.newValue;

    return (
      <tr>
        <td>
          <Input type="text"
                 name="newKey"
                 id="newKey"
                 data-testid="newKey"
                 bsSize="small"
                 placeholder={this.props.headers[0]}
                 value={this.state.newKey}
                 onChange={this._bindValue} />
        </td>
        <td>
          <Input type="text"
                 name="newValue"
                 id="newValue"
                 data-testid="newValue"
                 bsSize="small"
                 placeholder={this.props.headers[1]}
                 value={this.state.newValue}
                 onChange={this._bindValue} />
        </td>
        <td>
          <Button bsStyle="success" bsSize="small" onClick={this._addRow} disabled={addRowDisabled}>Add</Button>
        </td>
      </tr>
    );
  };

  render() {
    return (
      <div className="key-value-table-component">
        <div className={`table-responsive ${this.props.containerClassName}`}>
          <table className={`table table-striped ${this.props.className}`}>
            <thead>{this._formattedHeaders(this.props.headers)}</thead>
            <tbody>
              {this._formattedRows(this.props.pairs)}
              {this._newRow()}
            </tbody>
          </table>
        </div>
      </div>
    );
  }
}

export default KeyValueTable;
