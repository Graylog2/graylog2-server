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
import isEmpty from 'lodash/isEmpty';
import styled from 'styled-components';

import HumanReadableStreamRule from 'components/streamrules/HumanReadableStreamRule';
import { Icon } from 'components/common';
import { Button, ListGroupItem } from 'components/bootstrap';
import StreamRuleModal from 'components/streamrules/StreamRuleModal';
import UserNotification from 'util/UserNotification';
import useStreamRulesInputs from 'hooks/useStreamRulesInputs';
import useStreamRuleMutations from 'hooks/useStreamRuleMutations';
import type { StreamRule as StreamRuleTypeDefinition, Stream } from 'logic/streams/types';
import usePermissions from 'hooks/usePermissions';

const ActionButtonsWrap = styled.span`
  margin-right: 6px;
`;

type Props = {
  matchData?: {
    matches: boolean;
    rules: { [id: string]: false };
  };
  stream: Stream;
  onDelete?: (ruleId: string) => void;
  onSubmit?: (ruleId: string, data: unknown) => void;
  streamRule: StreamRuleTypeDefinition;
};

const StreamRule = ({ matchData = undefined, stream, streamRule, onSubmit = () => {}, onDelete = () => {} }: Props) => {
  const { t } = useTranslation('streams');
  const { isPermitted } = usePermissions();
  const [showStreamRuleForm, setShowStreamRuleForm] = useState(false);
  const { data: inputs } = useStreamRulesInputs();
  const { removeStreamRule, updateStreamRule } = useStreamRuleMutations();

  const _onEdit = (event: React.MouseEvent) => {
    event.preventDefault();
    setShowStreamRuleForm(true);
  };

  const _onDelete = (event: React.MouseEvent) => {
    event.preventDefault();

    /* TODO: Replace with custom confirmation dialog */
    // eslint-disable-next-line no-alert
    if (window.confirm(t('stream_rules.delete_confirm'))) {
      removeStreamRule({ streamId: stream.id, streamRuleId: streamRule.id }).then(() => {
        if (onDelete) {
          onDelete(streamRule.id);
        }

        UserNotification.success(t('stream_rules.deleted'), t('stream_rules.success_title'));
      });
    }
  };

  const _onSubmit = (streamRuleId: string, data: StreamRuleTypeDefinition) =>
    updateStreamRule({ streamId: stream.id, streamRuleId, data }).then(() => {
      if (onSubmit) {
        onSubmit(streamRuleId, data);
      }

      UserNotification.success(t('stream_rules.updated'), t('stream_rules.success_title'));
    });

  const _formatActionItems = () => (
    <ActionButtonsWrap>
      <Button bsStyle="link" bsSize="xsmall" onClick={_onDelete} title={t('stream_rules.delete_tooltip')}>
        <Icon name="delete" type="regular" />
      </Button>
      <Button bsStyle="link" bsSize="xsmall" onClick={_onEdit} title={t('stream_rules.edit_tooltip')}>
        <Icon name="edit_square" />
      </Button>
    </ActionButtonsWrap>
  );

  const matchDataStyle = () => (matchData.rules[streamRule.id] ? 'success' : 'danger');
  const actionItems = isPermitted([`streams:edit:${stream.id}`]) ? _formatActionItems() : null;
  const description = streamRule.description ? <small> ({streamRule.description})</small> : null;
  const listGroupStyle = !isEmpty(matchData) ? matchDataStyle() : null;

  return (
    <ListGroupItem bsStyle={listGroupStyle}>
      {actionItems}
      <HumanReadableStreamRule streamRule={streamRule} inputs={inputs} />
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
      {description}
    </ListGroupItem>
  );
};

export default StreamRule;
