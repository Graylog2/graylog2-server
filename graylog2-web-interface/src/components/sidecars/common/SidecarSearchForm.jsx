import React from 'react';
import PropTypes from 'prop-types';
import { Button, OverlayTrigger, Popover, Table } from 'react-bootstrap';

import { SearchForm } from 'components/common';
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
              <td>collector_version</td>
              <td>Sidecar version</td>
            </tr>
          </tbody>
        </Table>
        <p><strong>Examples</strong></p>
        <p>
          Find sidecars that did not communicate with Graylog since a date:<br />
          <kbd>{'last_seen:<=2018-04-10'}</kbd><br />
        </p>
        <p>
          Find sidecars with <code>failing</code> or <code>unknown</code> status:<br />
          <kbd>{'status:failing status:unknown'}</kbd><br />
        </p>
      </Popover>
    );

    const queryHelp = (
      <OverlayTrigger trigger="click" rootClose placement="right" overlay={queryHelpPopover}>
        <Button bsStyle="link"><i className="fa fa-question-circle" /></Button>
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
