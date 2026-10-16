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
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import { LinkContainer, DocumentTitle, PageHeader } from 'components/common';
import { Row, Col, Button, ButtonToolbar, BootstrapModalConfirm } from 'components/bootstrap';
import Spinner from 'components/common/Spinner';
import Routes from 'routing/Routes';
import UserNotification from 'util/UserNotification';
import ContentPackDetails from 'components/content-packs/ContentPackDetails';
import ContentPackVersions from 'components/content-packs/ContentPackVersions';
import ContentPackInstallations from 'components/content-packs/ContentPackInstallations';
import ContentPackInstallEntityList from 'components/content-packs/ContentPackInstallEntityList';
import {
  deleteContentPackRev,
  installContentPack,
  uninstallContentPack,
  fetchUninstallDetails,
} from 'hooks/useContentPackMutations';
import useHistory from 'routing/useHistory';
import useParams from 'routing/useParams';
import useContentPackRevisions from 'components/content-packs/hooks/useContentPackRevisions';
import useContentPackInstallations from 'components/content-packs/hooks/useContentPackInstallations';
import type FetchError from 'logic/errors/FetchError';
import MarketplaceLink from 'components/support/MarketplaceLink';
import type { EntitySharePayload } from 'actions/permissions/EntityShareActions';

import ShowContentPackStyle from './ShowContentPackPage.css';

const ShowContentPackPage = () => {
  const { t } = useTranslation('content_packs');
  const history = useHistory();
  const params = useParams<{ contentPackId: string }>();
  const onFetchError = (error: FetchError) => {
    if (error.status === 404) {
      UserNotification.error(t('show.not_found', { id: params.contentPackId }));
    } else {
      UserNotification.error(t('show.server_error'));
    }

    history.push(Routes.SYSTEM.CONTENTPACKS.LIST);
  };

  const [showModal, setShowModal] = useState(false);
  const [selectedVersion, setSelectedVersion] = useState(undefined);
  const [uninstallEntities, setUninstallEntities] = useState(undefined);
  const [uninstallContentPackId, setUninstallContentPackId] = useState(undefined);
  const [uninstallInstallId, setUninstallInstallId] = useState(undefined);
  const {
    data: contentPack,
    isInitialLoading: isLoadingContentPack,
    refetch: refetchContentPack,
  } = useContentPackRevisions(params?.contentPackId, onFetchError);
  const {
    data: contentPackInstallations,
    isInitialLoading: isLoadingInstallations,
    refetch: refetchInstallations,
  } = useContentPackInstallations(params?.contentPackId);

  const _onVersionChanged = (newVersion) => {
    setSelectedVersion(newVersion);
  };

  const _deleteContentPackRev = (contentPackId: string, revision?: number) => {
    /* eslint-disable-next-line no-alert */
    if (window.confirm(t('show.delete_revision_confirmation'))) {
      deleteContentPackRev(contentPackId, revision).then(
        () => {
          UserNotification.success(t('show.delete_revision_success'), t('notifications.success'));

          refetchContentPack();
        },
        (error) => {
          let errMessage = error.message;

          if (error.responseMessage) {
            errMessage = error.responseMessage;
          }

          UserNotification.error(t('show.delete_revision_error', { error: errMessage }), t('notifications.error'));
        },
      );
    }
  };

  const _onUninstallContentPackRev = (contentPackId: string, installId: string) => {
    fetchUninstallDetails(contentPackId, installId).then((result: { entities: unknown }) => {
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
    uninstallContentPack(uninstallContentPackId, uninstallInstallId).then(
      () => {
        UserNotification.success(t('show.uninstall_success'), t('notifications.success'));
        refetchInstallations();
        _clearUninstall();
      },
      () => {
        UserNotification.error(t('show.uninstall_error'), t('notifications.error'));
      },
    );
  };

  const _installContentPack = (
    contentPackId: string,
    contentPackRev: number,
    parameters: {},
    shareRequest: EntitySharePayload,
  ) => {
    installContentPack(contentPackId, contentPackRev, parameters, shareRequest).then(
      () => {
        UserNotification.success(t('overview.install_success'), t('notifications.success'));
        refetchInstallations();
      },
      (error) => {
        UserNotification.error(t('show.install_error', { status: error, id: contentPackId }));
      },
    );
  };

  if (isLoadingContentPack || isLoadingInstallations) {
    return <Spinner />;
  }

  const { contentPackRevisions, constraints, selectedVersion: currentVersion } = contentPack;
  const { installations } = contentPackInstallations;

  return (
    <DocumentTitle title={t('pages.document_title')}>
      <span>
        <PageHeader
          title={t('pages.document_title')}
          topActions={
            <ButtonToolbar>
              <LinkContainer to={Routes.SYSTEM.CONTENTPACKS.LIST}>
                <Button bsStyle="info">{t('pages.back_button')}</Button>
              </LinkContainer>
            </ButtonToolbar>
          }>
          <span>
            {t('pages.description')}
            <br />
            <MarketplaceLink prefix={t('pages.marketplace_prefix')} />
          </span>
        </PageHeader>

        <Row>
          <Col md={4} className="content">
            <div id="content-pack-versions">
              <Row className={ShowContentPackStyle.leftRow}>
                <Col>
                  <h2>{t('show.versions')}</h2>
                  <ContentPackVersions
                    contentPackRevisions={contentPackRevisions}
                    onInstall={_installContentPack}
                    onChange={_onVersionChanged}
                    onDeletePack={_deleteContentPackRev}
                  />
                </Col>
              </Row>
              <Row className={ShowContentPackStyle.leftRow}>
                <Col>
                  <h2>{t('show.installations')}</h2>
                  <ContentPackInstallations installations={installations} onUninstall={_onUninstallContentPackRev} />
                </Col>
              </Row>
            </div>
          </Col>
          <Col md={8} className="content">
            <ContentPackDetails
              contentPack={contentPackRevisions.contentPack(selectedVersion ?? currentVersion)}
              constraints={constraints[selectedVersion ?? currentVersion]}
              showConstraints
              verbose
            />
          </Col>
        </Row>
      </span>
      <BootstrapModalConfirm
        showModal={showModal}
        title={t('show.uninstall_confirmation')}
        onConfirm={_uninstallContentPackRev}
        onCancel={_clearUninstall}>
        <ContentPackInstallEntityList uninstall entities={uninstallEntities} />
      </BootstrapModalConfirm>
    </DocumentTitle>
  );
};

export default ShowContentPackPage;
