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
import PropTypes from 'prop-types';

import { OverlayTrigger, Popover, Table, Button } from 'components/graylog';
import { SearchForm, Icon } from 'components/common';

import style from './SidecarSearchForm.css';

class SidecarSearchForm extends React.Component {
  static propTypes = {
    query: PropTypes.string.isRequired,
    onSearch: PropTypes.func.isRequired,
    onReset: PropTypes.func.isRequired,
    children: PropTypes.element,
  };

  static defaultProps = {
    children: undefined,
  };

  render() {
    const { query, onSearch, onReset, children } = this.props;

    const queryHelpPopover = (
      <Popover id="search-query-help" className={style.popoverWide} title="Search Syntax Help">
        <p><strong>Available search fields</strong></p>
        <Table condensed>
          <thead>
            <tr>
              <th>Field</th>
              <th>Description</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>name</td>
              <td>Sidecar name</td>
            </tr>
            <tr>
              <td>status</td>
              <td>Status of the sidecar as it appears in the list, i.e. running, failing, or unknown</td>
            </tr>
            <tr>
              <td>operating_system</td>
              <td>Operating system the sidecar is running on</td>
            </tr>
            <tr>
              <td>last_seen</td>
              <td>Date and time when the sidecar last communicated with Graylog</td>
            </tr>
            <tr>
              <td>node_id</td>
              <td>Identifier of the sidecar</td>
            </tr>
            <tr>
              <td>sidecar_version</td>
              <td>Sidecar version</td>
            </tr>
          </tbody>
        </Table>
        <p><strong>Examples</strong></p>
        <p>
          Find sidecars that did not communicate with Graylog since a date:<br />
          <code>{'last_seen:<=2018-04-10'}</code><br />
        </p>
        <p>
          Find sidecars with <code>failing</code> or <code>unknown</code> status:<br />
          <code>status:failing status:unknown</code><br />
        </p>
      </Popover>
    );

    const queryHelp = (
      <OverlayTrigger trigger="click" rootClose placement="right" overlay={queryHelpPopover}>
        <Button bsStyle="link"><Icon name="question-circle" /></Button>
      </OverlayTrigger>
    );

    return (
      <SearchForm query={query}
                  onSearch={onSearch}
                  onReset={onReset}
                  searchButtonLabel="Find"
                  placeholder="Find sidecars"
                  queryWidth={400}
                  queryHelpComponent={queryHelp}
                  topMargin={0}
                  useLoadingState>
        {children}
      </SearchForm>
    );
  }
}

export default SidecarSearchForm;
