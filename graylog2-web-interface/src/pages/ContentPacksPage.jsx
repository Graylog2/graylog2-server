import React from 'react';
import { Row, Col, Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import Spinner from 'components/common/Spinner';
import Reflux from 'reflux';

import Routes from 'routing/Routes';

import { DocumentTitle, PageHeader } from 'components/common';
import ContentPacksList from 'components/content-packs/ContentPacksList';
import ContentPackUploadControls from 'components/content-packs/ContentPackUploadControls';
import ContentPackStores from 'stores/content-packs/ContentPackStores';

class ContentPacksPage extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      contentPacks: undefined,
    };
  }

  componentDidMount() {
    this._loadContentPacks();
  }

  _loadContentPacks() {
    ContentPackStores.list().then((contentPacks) => {
      this.setState({ contentPacks: contentPacks });
    });
  }

  render() {
    if (!this.state.contentPacks) {
      return (<Spinner />);
    }

    return (
      <DocumentTitle title="Content packs">
        <span>
          <PageHeader title="Content packs">
            <span>
              Content packs accelerate the set up process for a specific data source. A content pack can include inputs/extractors, streams, and dashboards.
            </span>

            <span>
              Find more content packs in {' '}
              <a href="https://marketplace.graylog.org/" target="_blank">the Graylog Marketplace</a>.
            </span>

            <div>
              <ContentPackUploadControls />
              <LinkContainer to={Routes.SYSTEM.CONTENTPACKS.EXPORT}>
                <Button bsStyle="success" bsSize="large">Create a content pack</Button>
              </LinkContainer>
            </div>
          </PageHeader>

          <Row className="content">
            <Col md={12}>

              <div id="react-configuration-bundles">
                <ContentPacksList
                  contentPacks={this.state.contentPacks}
                />
              </div>
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  }
}

export default ContentPacksPage;
