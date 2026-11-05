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

import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import styled, { css } from 'styled-components';

import UserNotification from 'util/UserNotification';
import HumanReadableStreamRule from 'components/streamrules/HumanReadableStreamRule';
import { ConfirmDialog, Icon } from 'components/common';
import { Button } from 'components/bootstrap';
import StreamRuleModal from 'components/streamrules/StreamRuleModal';
import useStreamRulesInputs from 'hooks/useStreamRulesInputs';
import useStreamRuleMutations from 'hooks/useStreamRuleMutations';
import type { StreamRule as StreamRuleTypeDefinition, Stream, StreamRule } from 'logic/streams/types';
import usePermissions from 'hooks/usePermissions';

const ActionButtonsWrap = styled.span(
  ({ theme }) => css`
    margin-right: ${theme.spacings.xs};
    float: right;
  `,
);

const StyledDeleteButton = styled(Button)(
  ({ theme }) => css`
    margin: 0 ${theme.spacings.xxs};
  `,
);

type Props = {
  stream: Stream;
  onDelete?: (ruleId: string) => void;
  onSubmit?: (ruleId: string, data: unknown) => void;
  streamRule: StreamRuleTypeDefinition;
};

const DetailsStreamRule = ({ stream, streamRule, onSubmit = () => {}, onDelete = () => {} }: Props) => {
  const { t } = useTranslation('streams');
  const { isPermitted } = usePermissions();
  const [showStreamRuleForm, setShowStreamRuleForm] = useState(false);
  const [showConfirmDelete, setShowConfirmDelete] = useState(false);
  const { data: inputs } = useStreamRulesInputs();
  const { removeStreamRule, updateStreamRule } = useStreamRuleMutations();

  const onConfirmDelete = () => {
    removeStreamRule({ streamId: stream.id, streamRuleId: streamRule.id }).then(() => {
      onDelete(streamRule.id);
      setShowConfirmDelete(false);
      UserNotification.success(t('stream_rules.deleted'), t('stream_rules.success_title'));
    });
  };

  const _onSubmit = (streamRuleId: string, data: StreamRule) =>
    updateStreamRule({ streamId: stream.id, streamRuleId, data }).then(() => {
      onSubmit(streamRuleId, data);
      UserNotification.success(t('stream_rules.updated'), t('stream_rules.success_title'));
    });

  const _formatActionItems = () => (
    <ActionButtonsWrap className="align-right">
      <StyledDeleteButton
        bsStyle="default"
        bsSize="xsmall"
        onClick={() => setShowStreamRuleForm(true)}
        title={t('stream_rules.edit_tooltip')}>
        <Icon name="edit_square" type="regular" />
      </StyledDeleteButton>
      <Button
        bsStyle="danger"
        bsSize="xsmall"
        onClick={() => setShowConfirmDelete(true)}
        title={t('stream_rules.delete_tooltip')}>
        <Icon name="delete" type="regular" />
      </Button>
    </ActionButtonsWrap>
  );

  const actionItems = isPermitted([`streams:edit:${stream.id}`]) ? _formatActionItems() : null;
  const description = streamRule.description ? <small> ({streamRule.description})</small> : null;

  return (
    <tr key={streamRule.id}>
      {}
      <td>
        <HumanReadableStreamRule streamRule={streamRule} inputs={inputs} />
        {description}
      </td>
      <td>{actionItems}</td>
      {showStreamRuleForm && (
        <StreamRuleModal
          initialValues={streamRule}
          onClose={() => setShowStreamRuleForm(false)}
          title={t('stream_rules.edit_title')}
          submitButtonText={t('stream_rules.update_button')}
          submitLoadingText={t('stream_rules.updating')}
          onSubmit={_onSubmit}
        />
      )}
      {showConfirmDelete && (
        <ConfirmDialog
          show={showConfirmDelete}
          onConfirm={onConfirmDelete}
          onCancel={() => setShowConfirmDelete(false)}
          title={t('stream_rules.delete_title')}
          btnConfirmText={t('stream_rules.delete_ok')}>
          {t('stream_rules.delete_confirm')}
        </ConfirmDialog>
      )}
    </tr>
  );
};

export default DetailsStreamRule;
