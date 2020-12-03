/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
import Reflux from 'reflux';
// eslint-disable-next-line no-restricted-imports
import createReactClass from 'create-react-class';
import styled, { css } from 'styled-components';

import { LinkContainer } from 'components/graylog/router';
import { Row, Col, ButtonToolbar, Button } from 'components/graylog';
import Routes from 'routing/Routes';
import Spinner from 'components/common/Spinner';
import UserNotification from 'util/UserNotification';
import { DocumentTitle, PageHeader } from 'components/common';
import ContentPacksList from 'components/content-packs/ContentPacksList';
import ContentPackUploadControls from 'components/content-packs/ContentPackUploadControls';
import CombinedProvider from 'injection/CombinedProvider';

const { ContentPacksActions, ContentPacksStore } = CombinedProvider.get('ContentPacks');

const ConfigurationBundles = styled.div(({ theme }) => css`
  font-size: ${theme.fonts.size.body};
  font-weight: normal;
  line-height: 20px;
  margin-top: 15px;
`);

const ContentPacksPage = createReactClass({
  displayName: 'ContentPacksPage',
  mixins: [Reflux.connect(ContentPacksStore)],

  componentDidMount() {
    ContentPacksActions.list();
  },

  _deleteContentPack(contentPackId) {
    // eslint-disable-next-line no-alert
    if (window.confirm('You are about to delete this Content Pack, are you sure?')) {
      ContentPacksActions.delete(contentPackId).then(() => {
        UserNotification.success('Content Pack deleted successfully.', 'Success');
        ContentPacksActions.list();
      }, (error) => {
        let err_message = error.message;
        const err_body = error.additional.body;

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
         Could not install Content Pack with ID: ${contentPackId}`);
    });
  },

  render() {
    const { contentPacks, contentPackMetadata } = this.state;

    if (!contentPacks) {
      return (<Spinner />);
    }

    return (
      <DocumentTitle title="Content Packs">
        <span>
          <PageHeader title="Content Packs">
            <span>
              Content Packs accelerate the set up process for a specific data source. A Content Pack can include inputs/extractors, streams, and dashboards.
            </span>

            <span>
              Find more Content Packs in {' '}
              <a href="https://marketplace.graylog.org/" target="_blank" rel="noopener noreferrer">the Graylog Marketplace</a>.
            </span>

            <ButtonToolbar>
              <ContentPackUploadControls />
              <LinkContainer to={Routes.SYSTEM.CONTENTPACKS.CREATE}>
                <Button bsStyle="success">Create a Content Pack</Button>
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
