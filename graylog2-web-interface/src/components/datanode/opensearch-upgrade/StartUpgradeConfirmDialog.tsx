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
import styled, { css } from 'styled-components';
import { Trans, useTranslation } from 'react-i18next';

import useProductName from 'brand-customization/useProductName';
import { Alert } from 'components/bootstrap';
import { ConfirmDialog } from 'components/common';

const Paragraph = styled.p(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.sm};

    &:last-child {
      margin-bottom: 0;
    }
  `,
);

type Props = {
  currentVersion: string | undefined;
  targetVersion: string | undefined;
  numberOfDataNodes: number;
  isRollingUpgrade: boolean;
  isSubmitting: boolean;
  onCancel: () => void;
  onConfirm: () => void;
};

const StartUpgradeConfirmDialog = ({
  currentVersion,
  targetVersion,
  numberOfDataNodes,
  isRollingUpgrade,
  isSubmitting,
  onCancel,
  onConfirm,
}: Props) => {
  const { t } = useTranslation('datanode');
  const productName = useProductName();
  const hasVersions = !!(currentVersion && targetVersion);
  const versionValues = { count: numberOfDataNodes, currentVersion, targetVersion };
  const strong = { strong: <strong /> };

  return isRollingUpgrade ? (
    <ConfirmDialog
      show
      title={t('start_upgrade.rolling.title')}
      btnConfirmText={t('start_upgrade.rolling.confirm')}
      isAsyncSubmit
      isSubmitting={isSubmitting}
      onCancel={onCancel}
      onConfirm={onConfirm}
      submitLoadingText={t('start_upgrade.starting')}>
      <Alert bsStyle="warning" title={t('start_upgrade.rolling.alert_title')}>
        <Paragraph>
          <Trans
            t={t}
            i18nKey={hasVersions ? 'start_upgrade.rolling.each_node_with_versions' : 'start_upgrade.rolling.each_node'}
            values={versionValues}
            components={strong}
          />
        </Paragraph>
        <Paragraph>{t('start_upgrade.rolling.shard_allocation')}</Paragraph>
        <Paragraph>{t('start_upgrade.rolling.wait_for_green')}</Paragraph>
        <Paragraph>{t('start_upgrade.no_rollback')}</Paragraph>
      </Alert>
      <p>{t('start_upgrade.rolling.healthy_cluster')}</p>
    </ConfirmDialog>
  ) : (
    <ConfirmDialog
      show
      title={t('start_upgrade.full.title')}
      btnConfirmText={t('start_upgrade.full.confirm')}
      isAsyncSubmit
      isSubmitting={isSubmitting}
      onCancel={onCancel}
      onConfirm={onConfirm}
      submitLoadingText={t('start_upgrade.full.submitting')}>
      <Alert bsStyle="danger" title={t('start_upgrade.full.alert_title')}>
        <Paragraph>
          <Trans
            t={t}
            i18nKey={
              hasVersions ? 'start_upgrade.full.restart_together_with_versions' : 'start_upgrade.full.restart_together'
            }
            values={versionValues}
            components={strong}
          />
        </Paragraph>
        <Paragraph>{t('start_upgrade.full.journal', { productName })}</Paragraph>
        <Paragraph>{t('start_upgrade.no_rollback')}</Paragraph>
      </Alert>
      <p>{t('start_upgrade.full.tolerate_downtime')}</p>
    </ConfirmDialog>
  );
};

export default StartUpgradeConfirmDialog;
