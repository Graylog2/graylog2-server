import React from 'react';
import Reflux from 'reflux';
import createReactClass from 'create-react-class';
import { LinkContainer } from 'react-router-bootstrap';
import styled from 'styled-components';

import { Row, Col, ButtonToolbar, Button } from 'components/graylog';
import Routes from 'routing/Routes';
import Spinner from 'components/common/Spinner';
import UserNotification from 'util/UserNotification';
import { DocumentTitle, PageHeader } from 'components/common';
import ContentPacksList from 'components/content-packs/ContentPacksList';
import ContentPackUploadControls from 'components/content-packs/ContentPackUploadControls';
import CombinedProvider from 'injection/CombinedProvider';

const { ContentPacksActions, ContentPacksStore } = CombinedProvider.get('ContentPacks');

const ConfigurationBundles = styled.div`
  font-size: 14px;
  font-weight: normal;
  line-height: 20px;
  margin-top: 15px;
`;

const ContentPacksPage = createReactClass({
  displayName: 'ContentPacksPage',
  mixins: [Reflux.connect(ContentPacksStore)],

  componentDidMount() {
    ContentPacksActions.list();
  },

  _deleteContentPack(contentPackId) {
    // eslint-disable-next-line no-alert
    if (window.confirm('You are about to delete this content pack, are you sure?')) {
      ContentPacksActions.delete(contentPackId).then(() => {
        UserNotification.success('Content Pack deleted successfully.', 'Success');
        ContentPacksActions.list();
      }, (error) => {
        /* eslint-disable camelcase */
        let err_message = error.message;
        const err_body = error.additional.body;
        /* eslint-enable camlecase */
        if (err_body && err_body.message) {
          err_message = error.additional.body.message;
        }
        UserNotification.error(`Deleting bundle failed: ${err_message}`, 'Error');
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
    const { contentPacks, contentPackMetadata } = this.state;

    if (!contentPacks) {
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
              <Button bsStyle="info" active>Content Packs</Button>
            </ButtonToolbar>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <ConfigurationBundles>
                <ContentPacksList contentPacks={contentPacks}
                                  contentPackMetadata={contentPackMetadata}
                                  onDeletePack={this._deleteContentPack}
                                  onInstall={this._installContentPack} />
              </ConfigurationBundles>
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  },
});

export default ContentPacksPage;
