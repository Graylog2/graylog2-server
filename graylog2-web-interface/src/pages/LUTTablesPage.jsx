import React, { PropTypes } from 'react';
import Reflux from 'reflux';
import { Button, Col, Row } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import Routes from 'routing/Routes';

import { DocumentTitle, PageHeader, Spinner } from 'components/common';

import { LookupTable, LookupTableCreate, LookupTableForm, LookupTablesOverview } from 'components/lookup-tables';

import CombinedProvider from 'injection/CombinedProvider';

const { LookupTablesStore, LookupTablesActions } = CombinedProvider.get('LookupTables');

const LUTTablesPage = React.createClass({
  propTypes: {
    history: PropTypes.object.isRequired,
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
    this._stopErrorStatesTimer();
    this.errorStatesTimer = setInterval(() => {
      let tableNames = null;
      if (this.state.tables) {
        tableNames = this.state.tables.map(t => t.name);
      }
      if (tableNames) {
        const adapterNames = Object.values(this.state.dataAdapters).map(a => a.name);
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
    this._stopErrorStatesTimer();
    if (props.params && props.params.tableName) {
      LookupTablesActions.get(props.params.tableName);
    } else if (this._isCreating(props)) {
      // nothing to do, the intermediate data container will take care of loading the caches and adapters
    } else {
      const p = this.state.pagination;
      LookupTablesActions.searchPaginated(p.page, p.per_page, p.query);
      this._startErrorStatesTimer();
    }
  },

  _saved() {
    // reset detail state
    this.setState({ table: undefined });
    this.props.history.pushState(null, Routes.SYSTEM.LOOKUPTABLES.OVERVIEW);
  },

  _isCreating(props) {
    return props.route.action === 'create';
  },

  _validateTable(table) {
    LookupTablesActions.validate(table);
  },

  render() {
    let content;
    const isShowing = this.props.route.action === 'show';
    const isEditing = this.props.route.action === 'edit';

    if (isShowing || isEditing) {
      if (!this.state.table) {
        content = <Spinner text="Loading lookup table" />;
      } else if (isEditing) {
        content = (
          <Row className="content">
            <Col lg={8}>
              <h2>Lookup Table</h2>
              <LookupTableForm table={this.state.table}
                               create={false}
                               saved={this._saved}
                               validate={this._validateTable}
                               validationErrors={this.state.validationErrors} />
            </Col>
          </Row>
        );
      } else {
        content = (<LookupTable dataAdapter={this.state.dataAdapter}
                                cache={this.state.cache}
                                table={this.state.table} />);
      }
    } else if (this._isCreating(this.props)) {
      content = (<LookupTableCreate history={this.props.history}
                                    saved={this._saved}
                                    validate={this._validateTable}
                                    validationErrors={this.state.validationErrors} />);
    } else if (!this.state || !this.state.tables) {
      content = <Spinner text="Loading lookup tables" />;
    } else {
      content = (<LookupTablesOverview tables={this.state.tables}
                                       caches={this.state.caches}
                                       dataAdapters={this.state.dataAdapters}
                                       pagination={this.state.pagination}
                                       errorStates={this.state.errorStates} />);
    }

    return (
      <DocumentTitle title="Lookup Tables">
        <span>
          <PageHeader title="Lookup Tables">
            <span>Lookup tables can be used in extractors, converters and processing pipelines to translate message fields or to enrich messages.</span>
            {null}
            <span>
              {isShowing && (
                <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.edit(this.props.params.tableName)}
                               onlyActiveOnIndex>
                  <Button bsStyle="success">Edit</Button>
                </LinkContainer>
              )}
              &nbsp;
              {(isShowing || isEditing) && (
                <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.OVERVIEW} onlyActiveOnIndex>
                  <Button bsStyle="info">Lookup Tables</Button>
                </LinkContainer>
              )}
              &nbsp;
              <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.CACHES.OVERVIEW} onlyActiveOnIndex>
                <Button bsStyle="info">Caches</Button>
              </LinkContainer>
              &nbsp;
              <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.OVERVIEW}
                             onlyActiveOnIndex>
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
