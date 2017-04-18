import React, { PropTypes } from 'react';
import Reflux from 'reflux';
import { Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import Routes from 'routing/Routes';

import { DocumentTitle, PageHeader, Spinner } from 'components/common';

import { LookupTablesOverview, LookupTable } from 'components/lookup-tables';

import CombinedProvider from 'injection/CombinedProvider';

const { LookupTablesStore, LookupTablesActions } = CombinedProvider.get('LookupTables');

const LUTTablesPage = React.createClass({
  propTypes: {
    params: PropTypes.object.isRequired,
  },

  mixins: [
    Reflux.connect(LookupTablesStore),
  ],

  componentDidMount() {
    if (this.props.params && this.props.params.tableName) {
      this._refresh(this.props.params.tableName);
    }
  },

  componentWillReceiveProps(nextProps) {
    if (nextProps.params && nextProps.params.tableName) {
      this._refresh(nextProps.params.tableName);
    }
  },

  _refresh(tableName) {
    LookupTablesActions.get(tableName);
  },

  render() {
    let content;
    const showDetail = this.props.params && this.props.params.tableName;
    if (showDetail) {
      if (this.state.tables.length > 0) {
        const table = this.state.tables[0];
        content = <LookupTable table={table} dataAdapter={this.state.dataAdapters[table.data_adapter_id]} cache={this.state.caches[table.cache_id]} />;
      } else {
        content = <Spinner text="Loading Lookup Table" />;
      }
    } else {
      content = <LookupTablesOverview />;
    }

    return (
      <DocumentTitle title="Lookup Tables">
        <span>
          <PageHeader title="Lookup Tables">
            <span>Looking things up</span>
            {null}
            <span>
              {showDetail && (
                <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.OVERVIEW} onlyActiveOnIndex>
                  <Button bsStyle="info">Lookup Tables</Button>
                </LinkContainer>
              )}
              &nbsp;
              <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.CACHES.OVERVIEW} onlyActiveOnIndex>
                <Button bsStyle="info">Caches</Button>
              </LinkContainer>
              &nbsp;
              <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.OVERVIEW} onlyActiveOnIndex>
                <Button bsStyle="info">Data Adapters</Button>
              </LinkContainer>
            </span>
          </PageHeader>

          {content}
        </span>
      </DocumentTitle>
    );
  },
});

export default LUTTablesPage;
