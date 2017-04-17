import React, { PropTypes } from 'react';
import { Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import Routes from 'routing/Routes';

import { DocumentTitle, PageHeader } from 'components/common';

const LUTDataAdaptersPage = React.createClass({
  propTypes: {
    params: PropTypes.object.isRequired,
  },

  render() {
    let content;
    const showDetail = this.props.params && this.props.params.adapterName;
    if (showDetail) {
      content = <span>Info about {this.props.params.adapterName}</span>;
    } else {
      content = <span>The list</span>;
    }

    return (
      <DocumentTitle title="Lookup Tables - Data Adapters">
        <span>
          <PageHeader title="Data adapters for Lookup Tables">
            <span>Data adapters provide the actual values for lookup tables</span>
            {null}
            <span>
              {showDetail && (
                <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.OVERVIEW} onlyActiveOnIndex>
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
