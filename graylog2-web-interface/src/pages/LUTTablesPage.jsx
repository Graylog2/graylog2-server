import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { LinkContainer } from 'react-router-bootstrap';

import Routes from 'routing/Routes';
import history from 'util/History';

import { ButtonToolbar, Col, Row, Button } from 'components/graylog';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { LookupTable, LookupTableCreate, LookupTableForm, LookupTablesOverview } from 'components/lookup-tables';

import CombinedProvider from 'injection/CombinedProvider';

const { LookupTablesStore, LookupTablesActions } = CombinedProvider.get('LookupTables');

const LUTTablesPage = createReactClass({
  displayName: 'LUTTablesPage',

  propTypes: {
    // eslint-disable-next-line react/no-unused-prop-types
    params: PropTypes.object.isRequired,
    route: PropTypes.object.isRequired,
  },

  mixins: [
    Reflux.connect(LookupTablesStore),
  ],

  componentDidMount() {
    this._loadData(this.props);
  },

  componentWillReceiveProps(nextProps) {
    this._loadData(nextProps);
  },

  componentWillUnmount() {
    clearInterval(this.errorStatesTimer);
  },

  errorStatesTimer: undefined,
  errorStatesInterval: 1000,

  _startErrorStatesTimer() {
    const { tables, dataAdapters } = this.state;
    this._stopErrorStatesTimer();
    this.errorStatesTimer = setInterval(() => {
      let tableNames = null;
      if (tables) {
        tableNames = tables.map(t => t.name);
      }
      if (tableNames) {
        const adapterNames = Object.values(dataAdapters).map(a => a.name);
        LookupTablesActions.getErrors(tableNames, null, adapterNames || null);
      }
    }, this.errorStatesInterval);
  },

  _stopErrorStatesTimer() {
    if (this.errorStatesTimer) {
      clearInterval(this.errorStatesTimer);
      this.errorStatesTimer = undefined;
    }
  },

  _loadData(props) {
    const { pagination } = this.state;
    this._stopErrorStatesTimer();
    if (props.params && props.params.tableName) {
      LookupTablesActions.get(props.params.tableName);
    } else if (this._isCreating(props)) {
      // nothing to do, the intermediate data container will take care of loading the caches and adapters
    } else {
      LookupTablesActions.searchPaginated(pagination.page, pagination.per_page, pagination.query);
      this._startErrorStatesTimer();
    }
  },

  _saved() {
    // reset detail state
    this.setState({ table: undefined });
    history.push(Routes.SYSTEM.LOOKUPTABLES.OVERVIEW);
  },

  _isCreating(props) {
    return props.route.action === 'create';
  },

  _validateTable(table) {
    LookupTablesActions.validate(table);
  },

  render() {
    const { route: { action } } = this.props;
    const {
      table,
      validationErrors,
      dataAdapter,
      cache,
      tables,
      caches,
      dataAdapters,
      pagination,
      errorStates,
    } = this.state;
    let content;
    const isShowing = action === 'show';
    const isEditing = action === 'edit';

    if (isShowing || isEditing) {
      if (!table) {
        content = <Spinner text="Loading lookup table" />;
      } else if (isEditing) {
        content = (
          <Row className="content">
            <Col lg={8}>
              <h2>Lookup Table</h2>
              <LookupTableForm table={table}
                               create={false}
                               saved={this._saved}
                               validate={this._validateTable}
                               validationErrors={validationErrors} />
            </Col>
          </Row>
        );
      } else {
        content = (
          <LookupTable dataAdapter={dataAdapter}
                       cache={cache}
                       table={table} />
        );
      }
    } else if (this._isCreating(this.props)) {
      content = (
        <LookupTableCreate saved={this._saved}
                           validate={this._validateTable}
                           validationErrors={validationErrors} />
      );
    } else if (!this.state || !tables) {
      content = <Spinner text="Loading lookup tables" />;
    } else {
      content = (
        <LookupTablesOverview tables={tables}
                              caches={caches}
                              dataAdapters={dataAdapters}
                              pagination={pagination}
                              errorStates={errorStates} />
      );
    }

    return (
      <DocumentTitle title="Lookup Tables">
        <span>
          <PageHeader title="Lookup Tables">
            <span>Lookup tables can be used in extractors, converters and processing pipelines to translate message fields or to enrich messages.</span>
            {null}
            <span>
              <ButtonToolbar>
                <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.OVERVIEW}>
                  <Button bsStyle="info" className="active">Lookup Tables</Button>
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
  },
});

export default LUTTablesPage;
