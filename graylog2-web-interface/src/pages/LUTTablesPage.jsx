import React, { PropTypes } from 'react';
import { Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import Routes from 'routing/Routes';

import { DocumentTitle, PageHeader } from 'components/common';

import { LookupTablesOverview } from 'components/lookup-tables';

const LUTTablesPage = React.createClass({
  propTypes: {
    params: PropTypes.object.isRequired,
  },

  render() {
    let content;
    const showDetail = this.props.params && this.props.params.tableName;
    if (showDetail) {
      content = <span>Info about {this.props.params.tableName}</span>;
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
