import React, { PropTypes } from 'react';
import Reflux from 'reflux';
import { Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import Routes from 'routing/Routes';

import { DocumentTitle, PageHeader, Spinner } from 'components/common';

import { DataAdaptersOverview, DataAdapter } from 'components/lookup-tables';

import CombinedProvider from 'injection/CombinedProvider';

const { LookupTableDataAdaptersStore, LookupTableDataAdaptersActions } = CombinedProvider.get('LookupTableDataAdapters');

const LUTDataAdaptersPage = React.createClass({
  propTypes: {
    params: PropTypes.object.isRequired,
  },

  mixins: [
    Reflux.connect(LookupTableDataAdaptersStore),
  ],

  componentDidMount() {
    if (this.props.params && this.props.params.adapterName) {
      this._refresh(this.props.params.adapterName);
    }
  },

  componentWillReceiveProps(nextProps) {
    if (nextProps.params && nextProps.params.adapterName) {
      this._refresh(nextProps.params.adapterName);
    }
  },

  _refresh(cacheName) {
    LookupTableDataAdaptersActions.get(cacheName);
  },

  render() {
    let content;
    const showDetail = this.props.params && this.props.params.adapterName;
    if (showDetail) {
      if (this.state.dataAdapters.length > 0) {
        content = <DataAdapter dataAdapter={this.state.dataAdapters[0]} />;
      } else {
        content = <Spinner text="Loading Lookup Table DataAdapter" />;
      }
    } else {
      content = <DataAdaptersOverview />;
    }

    return (
      <DocumentTitle title="Lookup Tables - Data Adapters">
        <span>
          <PageHeader title="Data adapters for Lookup Tables">
            <span>Data adapters provide the actual values for lookup tables</span>
            {null}
            <span>
              {showDetail && (
                <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.OVERVIEW} onlyActiveOnIndex>
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
