import React, { PropTypes } from 'react';
import Reflux from 'reflux';

import { Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import Routes from 'routing/Routes';

import { DocumentTitle, PageHeader, Spinner } from 'components/common';

import { CachesOverview, Cache } from 'components/lookup-tables';

import CombinedProvider from 'injection/CombinedProvider';

const { LookupTableCachesStore, LookupTableCachesActions } = CombinedProvider.get('LookupTableCaches');

const LUTCachesPage = React.createClass({
  propTypes: {
    params: PropTypes.object.isRequired,
  },

  mixins: [
    Reflux.connect(LookupTableCachesStore),
  ],

  componentDidMount() {
    if (this.props.params && this.props.params.cacheName) {
      this._refresh(this.props.params.cacheName);
    }
  },

  componentWillReceiveProps(nextProps) {
    if (nextProps.params && nextProps.params.cacheName) {
      this._refresh(nextProps.params.cacheName);
    }
  },

  _refresh(cacheName) {
    LookupTableCachesActions.get(cacheName);
  },

  render() {
    let content;
    const showDetail = this.props.params && this.props.params.cacheName;
    if (showDetail) {
      if (this.state.caches.length > 0) {
        content = <Cache cache={this.state.caches[0]} />;
      } else {
        content = <Spinner text="Loading Lookup Table Cache" />;
      }
    } else {
      content = <CachesOverview />;
    }

    return (
      <DocumentTitle title="Lookup Tables - Caches">
        <span>
          <PageHeader title="Caches for Lookup Tables">
            <span>Caches for lookup tables</span>
            {null}
            <span>
              {showDetail && (
                <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.CACHES.OVERVIEW} onlyActiveOnIndex>
                  <Button bsStyle="info">Caches</Button>
                </LinkContainer>
              )}
              &nbsp;
              <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.OVERVIEW} onlyActiveOnIndex>
                <Button bsStyle="info">Lookup Tables</Button>
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

export default LUTCachesPage;
