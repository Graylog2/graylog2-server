import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { Button, ButtonToolbar, Col, Row } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import Routes from 'routing/Routes';
import history from 'util/History';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';

import { DataAdapter, DataAdapterCreate, DataAdapterForm, DataAdaptersOverview } from 'components/lookup-tables';

import CombinedProvider from 'injection/CombinedProvider';

const { LookupTableDataAdaptersStore, LookupTableDataAdaptersActions } = CombinedProvider.get(
  'LookupTableDataAdapters');
const { LookupTablesStore, LookupTablesActions } = CombinedProvider.get('LookupTables');

const LUTDataAdaptersPage = createReactClass({
  displayName: 'LUTDataAdaptersPage',

  propTypes: {
    // eslint-disable-next-line react/no-unused-prop-types
    params: PropTypes.object.isRequired,
    route: PropTypes.object.isRequired,
  },

  mixins: [
    Reflux.connect(LookupTableDataAdaptersStore),
    Reflux.connect(LookupTablesStore, 'tableStore'),
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
      let names = null;
      if (this.state.dataAdapters) {
        names = this.state.dataAdapters.map(t => t.name);
      }
      if (names) {
        LookupTablesActions.getErrors(null, null, names || null);
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
    if (props.params && props.params.adapterName) {
      LookupTableDataAdaptersActions.get(props.params.adapterName);
    } else if (this._isCreating(props)) {
      LookupTableDataAdaptersActions.getTypes();
    } else {
      const p = this.state.pagination;
      LookupTableDataAdaptersActions.searchPaginated(p.page, p.per_page, p.query);
      this._startErrorStatesTimer();
    }
  },

  _saved() {
    // reset detail state
    this.setState({ dataAdapter: undefined });
    history.push(Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.OVERVIEW);
  },

  _isCreating(props) {
    return props.route.action === 'create';
  },

  _validateAdapter(adapter) {
    LookupTableDataAdaptersActions.validate(adapter);
  },

  render() {
    let content;
    const isShowing = this.props.route.action === 'show';
    const isEditing = this.props.route.action === 'edit';

    if (isShowing || isEditing) {
      if (!this.state.dataAdapter) {
        content = <Spinner text="Loading data adapter" />;
      } else if (isEditing) {
        content = (
          <Row className="content">
            <Col lg={12}>
              <h2>Data Adapter</h2>
              <DataAdapterForm dataAdapter={this.state.dataAdapter}
                               type={this.state.dataAdapter.config.type}
                               create={false}
                               saved={this._saved}
                               validate={this._validateAdapter}
                               validationErrors={this.state.validationErrors} />
            </Col>
          </Row>
        );
      } else {
        content = <DataAdapter dataAdapter={this.state.dataAdapter} />;
      }
    } else if (this._isCreating(this.props)) {
      if (!this.state.types) {
        content = <Spinner text="Loading data adapter types" />;
      } else {
        content = (<DataAdapterCreate types={this.state.types}
                                      saved={this._saved}
                                      validate={this._validateAdapter}
                                      validationErrors={this.state.validationErrors} />);
      }
    } else if (!this.state.dataAdapters) {
      content = <Spinner text="Loading data adapters" />;
    } else {
      content = (<DataAdaptersOverview dataAdapters={this.state.dataAdapters}
                                       pagination={this.state.pagination} errorStates={this.state.tableStore.errorStates} />);
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
                  <Button bsStyle="info" className="active">Data Adapters</Button>
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

export default LUTDataAdaptersPage;
