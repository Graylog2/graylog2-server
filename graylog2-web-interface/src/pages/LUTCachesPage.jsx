import React, { PropTypes } from 'react';
import { Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import Routes from 'routing/Routes';

import { DocumentTitle, PageHeader } from 'components/common';

const LUTCachesPage = React.createClass({
  propTypes: {
    params: PropTypes.object.isRequired,
  },

  render() {
    let content;
    const showDetail = this.props.params && this.props.params.cacheName;
    if (showDetail) {
      content = <span>Info about {this.props.params.cacheName}</span>;
    } else {
      content = <span>The list</span>;
    }

    return (
      <DocumentTitle title="Lookup Tables - Caches">
        <span>
          <PageHeader title="Caches for Lookup Tables">
            <span>Caches for lookup tables</span>
            {null}
            <span>
              {showDetail && (
                <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.OVERVIEW} onlyActiveOnIndex>
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
