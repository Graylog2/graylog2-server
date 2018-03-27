import React from 'react';
import createReactClass from 'create-react-class';

import Routes from 'routing/Routes';
import { Row, Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import { DocumentTitle, PageHeader } from 'components/common';

const CreateContentPackPage = createReactClass({
  displayName: 'ShowContentPackPage',

  render() {
    return (
      <DocumentTitle title="Content packs">
        <span>
          <PageHeader title="Create content packs">
            <span>
              Content packs accelerate the set up process for a specific data source. A content pack can include inputs/extractors, streams, and dashboards.
            </span>

            <span>
              Find more content packs in {' '}
              <a href="https://marketplace.graylog.org/" target="_blank">the Graylog Marketplace</a>.
            </span>

            <div>
              <LinkContainer to={Routes.SYSTEM.CONTENTPACKS.LIST}>
                <Button bsStyle="info" bsSize="large">Content Packs</Button>
              </LinkContainer>
            </div>
          </PageHeader>
          <Row />
        </span>
      </DocumentTitle>
    );
  },

});

export default CreateContentPackPage;
