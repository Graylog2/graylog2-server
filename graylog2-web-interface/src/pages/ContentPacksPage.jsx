import React from 'react';
import Reflux from 'reflux';
import createReactClass from 'create-react-class';
import Routes from 'routing/Routes';

import Spinner from 'components/common/Spinner';
import { Row, Col, Button, ButtonToolbar } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import UserNotification from 'util/UserNotification';
import { DocumentTitle, PageHeader } from 'components/common';
import ContentPacksList from 'components/content-packs/ContentPacksList';
import ContentPackUploadControls from 'components/content-packs/ContentPackUploadControls';
import CombinedProvider from 'injection/CombinedProvider';

const { ContentPacksActions, ContentPacksStore } = CombinedProvider.get('ContentPacks');

const ContentPacksPage = createReactClass({
  displayName: 'ContentPacksPage',
  mixins: [Reflux.connect(ContentPacksStore)],

  componentDidMount() {
    ContentPacksActions.list();
  },

  _deleteContentPack(contentPackId) {
    if (window.confirm('You are about to delete this content pack, are you sure?')) {
      ContentPacksActions.delete(contentPackId).then(() => {
        UserNotification.success('Content Pack deleted successfully.', 'Success');
        ContentPacksActions.list();
      }, () => {
        UserNotification.error('Deleting bundle failed, please check your logs for more information.', 'Error');
      });
    }
  },

  _installContentPack(contentPackId, contentPackRev, parameters) {
    ContentPacksActions.install(contentPackId, contentPackRev, parameters).then(() => {
      UserNotification.success('Content Pack installed successfully.', 'Success');
    }, (error) => {
      UserNotification.error(`Installing content pack failed with status: ${error}.
         Could not install content pack with ID: ${contentPackId}`);
    });
  },

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
              <a href="https://marketplace.graylog.org/" target="_blank" rel="noopener noreferrer">the Graylog Marketplace</a>.
            </span>

            <ButtonToolbar>
              <ContentPackUploadControls />
              <LinkContainer to={Routes.SYSTEM.CONTENTPACKS.CREATE}>
                <Button bsStyle="success">Create a content pack</Button>
              </LinkContainer>
            </ButtonToolbar>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <div id="react-configuration-bundles">
                <ContentPacksList
                  contentPacks={this.state.contentPacks}
                  onDeletePack={this._deleteContentPack}
                  onInstall={this._installContentPack}
                />
              </div>
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  },
});

export default ContentPacksPage;
