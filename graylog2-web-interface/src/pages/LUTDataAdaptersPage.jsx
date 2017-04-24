import React, { PropTypes } from 'react';
import Reflux from 'reflux';
import { Button, Row, Col } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import Routes from 'routing/Routes';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';

import {
  DataAdaptersOverview, DataAdapter, DataAdapterForm, DataAdapterCreate
} from 'components/lookup-tables';

import CombinedProvider from 'injection/CombinedProvider';

const { LookupTableDataAdaptersStore, LookupTableDataAdaptersActions } = CombinedProvider.get(
  'LookupTableDataAdapters');

const LUTDataAdaptersPage = React.createClass({
  propTypes: {
// eslint-disable-next-line react/no-unused-prop-types
    params: PropTypes.object.isRequired,
    route: PropTypes.object.isRequired,
    history: PropTypes.object.isRequired,
  },

  mixins: [
    Reflux.connect(LookupTableDataAdaptersStore),
  ],

  componentDidMount() {
    this._loadData(this.props);
  },

  componentWillReceiveProps(nextProps) {
    this._loadData(nextProps);
  },

  _loadData(props) {
    if (props.params && props.params.adapterName) {
      LookupTableDataAdaptersActions.get(props.params.adapterName);
    } else {
      const p = this.state.pagination;
      LookupTableDataAdaptersActions.searchPaginated(p.page, p.per_page, p.query);
    }
    if (this._isCreating(props)) {
      LookupTableDataAdaptersActions.getTypes();
    }
  },

  _saved() {
    this.props.history.pushState(null, Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.OVERVIEW);
  },

  _isCreating(props) {
    return props.route.action === 'create';
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
            <Col lg={8}>
              <h2>Data Adapter</h2>
              <DataAdapterForm dataAdapter={this.state.dataAdapter}
                               type={this.state.dataAdapter.config.type}
                               create={false}
                               saved={this._saved} />
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
        content = <DataAdapterCreate history={this.props.history} types={this.state.types} saved={this._saved} />;
      }
    } else if (!this.state.dataAdapters) {
      content = <Spinner text="Loading data adapters" />;
    } else {
      content = (<DataAdaptersOverview dataAdapters={this.state.dataAdapters}
                                       pagination={this.state.pagination} />);
    }

    return (
      <DocumentTitle title="Lookup Tables - Data Adapters">
        <span>
          <PageHeader title="Data adapters for Lookup Tables">
            <span>Data adapters provide the actual values for lookup tables</span>
            {null}
            <span>
              {(isShowing || isEditing) && (
                <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.OVERVIEW}
                               onlyActiveOnIndex>
                  <Button bsStyle="info">Data Adapters</Button>
                </LinkContainer>
              )}
              &nbsp;
              <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.OVERVIEW} onlyActiveOnIndex>
                <Button bsStyle="info">Lookup Tables</Button>
              </LinkContainer>
              &nbsp;
              <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.CACHES.OVERVIEW} onlyActiveOnIndex>
                <Button bsStyle="info">Caches</Button>
              </LinkContainer>
            </span>
          </PageHeader>

          {content}
        </span>
      </DocumentTitle>
    );
  },
});

export default LUTDataAdaptersPage;
