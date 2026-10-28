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
import { Trans, useTranslation } from 'react-i18next';

import useProductName from 'brand-customization/useProductName';
import { Alert, Button, Modal } from 'components/bootstrap';
import { ExternalLink } from 'components/common';
import useRunsWithDataNode from 'components/datanode/hooks/useRunsWithDataNode';
import useIncompatibleIndices from 'components/indices/hooks/useIncompatibleIndices';
import DataNodeMigrationLink from 'components/indices/incompatible-indices/DataNodeMigrationLink';
import IncompatibleIndicesTable from 'components/indices/incompatible-indices/IncompatibleIndicesTable';
import useSearchVersionCheck from 'hooks/useSearchVersionCheck';
import DocsHelper from 'util/DocsHelper';

const MigrationLink = ({ children = undefined }: React.PropsWithChildren<{}>) => (
  <DataNodeMigrationLink text={children} />
);

const DocsLink = ({ children = undefined }: React.PropsWithChildren<{}>) => {
  const { t } = useTranslation('indexsets');

  return (
    <ExternalLink href={DocsHelper.toString(DocsHelper.PAGES.DATA_NODE_MIGRATION)}>
      {children}
      <span className="sr-only">{t('indices.incompatible_modal.opens_in_new_tab')}</span>
    </ExternalLink>
  );
};

type Props = {
  show: boolean;
  onClose: () => void;
};

const IncompatibleIndicesModal = ({ show, onClose }: Props) => {
  const { t } = useTranslation('indexsets');
  const productName = useProductName();
  const { data: runsWithDataNode } = useRunsWithDataNode();
  const { data: incompatibleIndices } = useIncompatibleIndices();
  const { data: openSearchVersionCheck } = useSearchVersionCheck('opensearch', '^2.0.0');
  const showUnsupportedUpgradeWarning =
    runsWithDataNode === false && openSearchVersionCheck?.satisfied === true && incompatibleIndices.length > 0;

  return (
    <Modal show={show} onHide={onClose} bsSize="xl">
      <Modal.Header>
        <Modal.Title>{t('indices.incompatible_modal.title')}</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {showUnsupportedUpgradeWarning && (
          <Alert bsStyle="warning">
            <p>{t('indices.incompatible_modal.upgrade_blocked')}</p>
            <p>
              <Trans
                t={t}
                i18nKey="indices.incompatible_modal.upgrade_path"
                components={{ migrationLink: <MigrationLink />, docsLink: <DocsLink /> }}
              />
            </p>
            <p>{t('indices.incompatible_modal.unsupported_direct_upgrade', { productName })}</p>
            <p>{t('indices.incompatible_modal.support_until')}</p>
          </Alert>
        )}
        <Alert bsStyle="info">{t('indices.incompatible_modal.info')}</Alert>
        <IncompatibleIndicesTable withoutURLParams />
      </Modal.Body>
      <Modal.Footer>
        <Button onClick={onClose}>{t('indices.incompatible_modal.close')}</Button>
      </Modal.Footer>
    </Modal>
  );
};

export default IncompatibleIndicesModal;
