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
import * as React from 'react';
import { useEffect, useState } from 'react';

import { LinkContainer } from 'components/common/router';
import { Row, Col, Button, ButtonToolbar, BootstrapModalConfirm } from 'components/bootstrap';
import Spinner from 'components/common/Spinner';
import Routes from 'routing/Routes';
import UserNotification from 'util/UserNotification';
import { DocumentTitle, PageHeader } from 'components/common';
import ContentPackDetails from 'components/content-packs/ContentPackDetails';
import ContentPackVersions from 'components/content-packs/ContentPackVersions';
import ContentPackInstallations from 'components/content-packs/ContentPackInstallations';
import ContentPackInstallEntityList from 'components/content-packs/ContentPackInstallEntityList';
import { ContentPacksActions, ContentPacksStore } from 'stores/content-packs/ContentPacksStore';
import { useStore } from 'stores/connect';
import useHistory from 'routing/useHistory';
import useParams from 'routing/useParams';

import ShowContentPackStyle from './ShowContentPackPage.css';

const ShowContentPackPage = () => {
  const { contentPackRevisions, installations, constraints, selectedVersion: currentVersion } = useStore(ContentPacksStore);
  const history = useHistory();
  const params = useParams<{ contentPackId: string }>();

  const [showModal, setShowModal] = useState(false);
  const [selectedVersion, setSelectedVersion] = useState(undefined);
  const [uninstallEntities, setUninstallEntities] = useState(undefined);
  const [uninstallContentPackId, setUninstallContentPackId] = useState(undefined);
  const [uninstallInstallId, setUninstallInstallId] = useState(undefined);

  useEffect(() => {
    ContentPacksActions.get(params.contentPackId).catch((error) => {
      if (error.status === 404) {
        UserNotification.error(
          `Cannot find Content Pack with the id ${params.contentPackId} and may have been deleted.`,
        );
      } else {
        UserNotification.error('An internal server error occurred. Please check your logfiles for more information');
      }

      history.push(Routes.SYSTEM.CONTENTPACKS.LIST);
    });

    ContentPacksActions.installList(params.contentPackId);
  }, [history, params?.contentPackId]);

  const _onVersionChanged = (newVersion) => {
    setSelectedVersion(newVersion);
  };

  const _deleteContentPackRev = (contentPackId: string, revision?: number) => {
    /* eslint-disable-next-line no-alert */
    if (window.confirm('You are about to delete this content pack revision, are you sure?')) {
      ContentPacksActions.deleteRev(contentPackId, revision).then(() => {
        UserNotification.success('Content pack revision deleted successfully.', 'Success');

        ContentPacksActions.get(contentPackId).catch((error) => {
          if (error.status !== 404) {
            UserNotification.error('An internal server error occurred. Please check your logfiles for more information');
          }

          history.push(Routes.SYSTEM.CONTENTPACKS.LIST);
        });
      }, (error) => {
        let errMessage = error.message;

        if (error.responseMessage) {
          errMessage = error.responseMessage;
        }

        UserNotification.error(`Deleting content pack failed: ${errMessage}`, 'Error');
      });
    }
  };

  const _onUninstallContentPackRev = (contentPackId: string, installId: string) => {
    ContentPacksActions.uninstallDetails(contentPackId, installId).then((result: { entities: unknown }) => {
      setUninstallEntities(result.entities);
    });

    setShowModal(true);
    setUninstallContentPackId(contentPackId);
    setUninstallInstallId(installId);
  };

  const _clearUninstall = () => {
    setShowModal(false);
    setUninstallContentPackId(undefined);
    setUninstallInstallId(undefined);
    setUninstallEntities(undefined);
  };

  const _uninstallContentPackRev = () => {
    const contentPackId = uninstallContentPackId;

    ContentPacksActions.uninstall(uninstallContentPackId, uninstallInstallId).then(() => {
      UserNotification.success('Content Pack uninstalled successfully.', 'Success');
      ContentPacksActions.installList(contentPackId);
      _clearUninstall();
    }, () => {
      UserNotification.error('Uninstall content pack failed, please check your logs for more information.', 'Error');
    });
  };

  const _installContentPack = (contentPackId: string, contentPackRev: string, parameters) => {
    ContentPacksActions.install(contentPackId, contentPackRev, parameters).then(() => {
      UserNotification.success('Content Pack installed successfully.', 'Success');
      ContentPacksActions.installList(contentPackId);
    }, (error) => {
      UserNotification.error(`Installing content pack failed with status: ${error}.
         Could not install content pack with ID: ${contentPackId}`);
    });
  };

  if (!contentPackRevisions) {
    return (<Spinner />);
  }

  return (
    <DocumentTitle title="Content packs">
      <span>
        <PageHeader title="Content packs"
                    topActions={(
                      <ButtonToolbar>
                        <LinkContainer to={Routes.SYSTEM.CONTENTPACKS.LIST}>
                          <Button bsStyle="info">Content Packs</Button>
                        </LinkContainer>
                      </ButtonToolbar>
                      )}>
          <span>
            Content packs accelerate the set up process for a specific data source. A content pack can include inputs/extractors, streams, and dashboards.
            <br />
            Find more content packs in {' '}
            <a href="https://marketplace.graylog.org/" target="_blank" rel="noopener noreferrer">the Graylog Marketplace</a>.
          </span>
        </PageHeader>

        <Row>
          <Col md={4} className="content">
            <div id="content-pack-versions">
              <Row className={ShowContentPackStyle.leftRow}>
                <Col>
                  <h2>Versions</h2>
                  <ContentPackVersions contentPackRevisions={contentPackRevisions}
                                       onInstall={_installContentPack}
                                       onChange={_onVersionChanged}
                                       onDeletePack={_deleteContentPackRev} />
                </Col>
              </Row>
              <Row className={ShowContentPackStyle.leftRow}>
                <Col>
                  <h2>Installations</h2>
                  <ContentPackInstallations installations={installations}
                                            onUninstall={_onUninstallContentPackRev} />
                </Col>
              </Row>
            </div>
          </Col>
          <Col md={8} className="content">
            {/* @ts-ignore */}
            <ContentPackDetails contentPack={contentPackRevisions.contentPack(selectedVersion ?? currentVersion)}
                                constraints={constraints[selectedVersion ?? currentVersion]}
                                showConstraints
                                verbose />
          </Col>
        </Row>
      </span>
      <BootstrapModalConfirm showModal={showModal}
                             title="Do you really want to uninstall this Content Pack?"
                             onConfirm={_uninstallContentPackRev}
                             onCancel={_clearUninstall}>
        <ContentPackInstallEntityList uninstall entities={uninstallEntities} />
      </BootstrapModalConfirm>
    </DocumentTitle>
  );
};

export default ShowContentPackPage;
