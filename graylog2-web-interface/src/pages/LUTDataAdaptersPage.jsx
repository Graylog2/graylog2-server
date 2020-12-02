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

import { LinkContainer } from 'components/graylog/router';
import connect from 'stores/connect';
import Routes from 'routing/Routes';
import history from 'util/History';
import { ButtonToolbar, Col, Row, Button } from 'components/graylog';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { DataAdapter, DataAdapterCreate, DataAdapterForm, DataAdaptersOverview } from 'components/lookup-tables';
import CombinedProvider from 'injection/CombinedProvider';
import withParams from 'routing/withParams';
import withLocation from 'routing/withLocation';

const { LookupTableDataAdaptersStore, LookupTableDataAdaptersActions } = CombinedProvider.get(
  'LookupTableDataAdapters',
);
const { LookupTablesStore, LookupTablesActions } = CombinedProvider.get('LookupTables');

class LUTDataAdaptersPage extends React.Component {
  errorStatesTimer = undefined;

  errorStatesInterval = 1000;

  componentDidMount() {
    this._loadData(this.props);
  }

  componentDidUpdate(prevProps) {
    const { location: { pathname } } = this.props;
    const { location: { pathname: prevPathname } } = prevProps;

    if (pathname !== prevPathname) {
      this._loadData(this.props);
    }
  }

  componentWillUnmount() {
    clearInterval(this.errorStatesTimer);
  }

  _startErrorStatesTimer = () => {
    this._stopErrorStatesTimer();

    this.errorStatesTimer = setInterval(() => {
      const { dataAdapters } = this.props;
      let names = null;

      if (dataAdapters) {
        names = dataAdapters.map((t) => t.name);
      }

      if (names) {
        LookupTablesActions.getErrors(null, null, names || null);
      }
    }, this.errorStatesInterval);
  }

  _stopErrorStatesTimer = () => {
    if (this.errorStatesTimer) {
      clearInterval(this.errorStatesTimer);
      this.errorStatesTimer = undefined;
    }
  }

  _loadData = (props) => {
    const { pagination } = props;

    this._stopErrorStatesTimer();

    if (props.params && props.params.adapterName) {
      LookupTableDataAdaptersActions.get(props.params.adapterName);
    } else if (this._isCreating(props)) {
      LookupTableDataAdaptersActions.getTypes();
    } else {
      LookupTableDataAdaptersActions.searchPaginated(pagination.page, pagination.per_page, pagination.query);
      this._startErrorStatesTimer();
    }
  }

  _saved = () => {
    // reset detail state
    history.push(Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.OVERVIEW);
  }

  _isCreating = ({ action }) => {
    return action === 'create';
  }

  _validateAdapter = (adapter) => {
    LookupTableDataAdaptersActions.validate(adapter);
  }

  render() {
    const {
      action,
      errorStates,
      dataAdapter,
      validationErrors,
      types,
      dataAdapters,
      pagination,
    } = this.props;
    let content;
    const isShowing = action === 'show';
    const isEditing = action === 'edit';

    if (isShowing || isEditing) {
      if (!dataAdapter) {
        content = <Spinner text="Loading data adapter" />;
      } else if (isEditing) {
        content = (
          <Row className="content">
            <Col lg={12}>
              <DataAdapterForm dataAdapter={dataAdapter}
                               type={dataAdapter.config.type}
                               create={false}
                               title="Data Adapter"
                               saved={this._saved}
                               validate={this._validateAdapter}
                               validationErrors={validationErrors} />
            </Col>
          </Row>
        );
      } else {
        content = <DataAdapter dataAdapter={dataAdapter} />;
      }
    } else if (this._isCreating(this.props)) {
      if (!types) {
        content = <Spinner text="Loading data adapter types" />;
      } else {
        content = (
          <DataAdapterCreate types={types}
                             saved={this._saved}
                             validate={this._validateAdapter}
                             validationErrors={validationErrors} />
        );
      }
    } else if (!dataAdapters) {
      content = <Spinner text="Loading data adapters" />;
    } else {
      content = (
        <DataAdaptersOverview dataAdapters={dataAdapters}
                              pagination={pagination}
                              errorStates={errorStates} />
      );
    }

    return (
      <DocumentTitle title="Lookup Tables - Data Adapters">
        <span>
          <PageHeader title="Data adapters for Lookup Tables">
            <span>Data adapters provide the actual values for lookup tables</span>
            {null}
            <span>
              <ButtonToolbar>
                <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.OVERVIEW}>
                  <Button bsStyle="info">Lookup Tables</Button>
                </LinkContainer>
                <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.CACHES.OVERVIEW}>
                  <Button bsStyle="info">Caches</Button>
                </LinkContainer>
                <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.OVERVIEW}>
                  <Button bsStyle="info">Data Adapters</Button>
                </LinkContainer>
              </ButtonToolbar>
            </span>
          </PageHeader>

          {content}
        </span>
      </DocumentTitle>
    );
  }
}

LUTDataAdaptersPage.propTypes = {
  errorStates: PropTypes.object,
  dataAdapter: PropTypes.object,
  validationErrors: PropTypes.object,
  types: PropTypes.object,
  pagination: PropTypes.object,
  dataAdapters: PropTypes.array,
  location: PropTypes.object.isRequired,
  action: PropTypes.string,
};

LUTDataAdaptersPage.defaultProps = {
  errorStates: null,
  validationErrors: {},
  dataAdapters: null,
  types: null,
  pagination: null,
  dataAdapter: null,
  action: undefined,
};

export default connect(withParams(withLocation(LUTDataAdaptersPage)), { lookupTableStore: LookupTablesStore, dataAdaptersStore: LookupTableDataAdaptersStore },
  ({ dataAdaptersStore, lookupTableStore, ...otherProps }) => ({
    ...otherProps,
    ...dataAdaptersStore,
    errorStates: lookupTableStore.errorStates,
  }));
