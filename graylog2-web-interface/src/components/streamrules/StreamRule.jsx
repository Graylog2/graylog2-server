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
import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { isEmpty } from 'lodash';
import styled from 'styled-components';

import { useStore } from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';
import { Icon } from 'components/common';
import { Button, ListGroupItem } from 'components/graylog';
import { isPermitted } from 'util/PermissionsMixin';
import StreamRuleForm from 'components/streamrules/StreamRuleForm';
import HumanReadableStreamRule from 'components/streamrules/HumanReadableStreamRule';
import StoreProvider from 'injection/StoreProvider';
import UserNotification from 'util/UserNotification';

const { InputsStore, InputsActions } = CombinedProvider.get('Inputs');

const StreamRulesStore = StoreProvider.getStore('StreamRules');

const ActionButtonsWrap = styled.span`
  margin-right: 6px;
`;

const StreamRule = ({ matchData, permissions, stream, streamRule, streamRuleTypes, onSubmit, onDelete }) => {
  const [showStreamRuleForm, setShowStreamRuleForm] = useState(false);
  const { inputs } = useStore(InputsStore);

  useEffect(() => {
    InputsActions.list();
  }, []);

  const _onEdit = (event) => {
    event.preventDefault();
    setShowStreamRuleForm(true);
  };

  const _onDelete = (event) => {
    event.preventDefault();

    /* TODO: Replace with custom confirmation dialog */
    // eslint-disable-next-line no-alert
    if (window.confirm('Do you really want to delete this stream rule?')) {
      StreamRulesStore.remove(stream.id, streamRule.id, () => {
        if (onDelete) {
          onDelete(streamRule.id);
        }

        UserNotification.success('Stream rule has been successfully deleted.', 'Success');
      });
    }
  };

  const _onSubmit = (streamRuleId, data) => {
    StreamRulesStore.update(stream.id, streamRuleId, data, () => {
      if (onSubmit) {
        onSubmit(streamRuleId, data);
      }

      UserNotification.success('Stream rule has been successfully updated.', 'Success');
    });
  };

  const _formatActionItems = () => {
    return (
      <ActionButtonsWrap>
        <Button bsStyle="link"
                bsSize="xsmall"
                onClick={_onDelete}>
          <Icon name="trash-alt" type="regular" />
        </Button>
        <Button bsStyle="link"
                bsSize="xsmall"
                onClick={_onEdit}>
          <Icon name="edit" />
        </Button>
      </ActionButtonsWrap>
    );
  };

  const matchDataStyle = () => (matchData.rules[streamRule.id] ? 'success' : 'danger');
  const actionItems = isPermitted(permissions, [`streams:edit:${stream.id}`]) ? _formatActionItems() : null;
  const description = streamRule.description ? <small>{' '}({streamRule.description})</small> : null;
  const listGroupStyle = !isEmpty(matchData) ? matchDataStyle() : null;

  return (
    <ListGroupItem bsStyle={listGroupStyle}>
      {actionItems}
      <HumanReadableStreamRule streamRule={streamRule} streamRuleTypes={streamRuleTypes} inputs={inputs} />
      { showStreamRuleForm && (
        <StreamRuleForm streamRule={streamRule}
                        onClose={() => setShowStreamRuleForm(false)}
                        streamRuleTypes={streamRuleTypes}
                        title="Edit Stream Rule"
                        onSubmit={_onSubmit} />
      ) }
      {description}
    </ListGroupItem>
  );
};

StreamRule.propTypes = {
  matchData: PropTypes.shape({
    matches: PropTypes.bool,
    rules: PropTypes.object,
  }),
  onDelete: PropTypes.func,
  onSubmit: PropTypes.func,
  permissions: PropTypes.array.isRequired,
  stream: PropTypes.object.isRequired,
  streamRule: PropTypes.object.isRequired,
  streamRuleTypes: PropTypes.array.isRequired,
};

StreamRule.defaultProps = {
  matchData: {},
  onSubmit: () => {},
  onDelete: () => {},
};

export default StreamRule;
