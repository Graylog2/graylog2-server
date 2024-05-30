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
import styled from 'styled-components';

import HumanReadableStreamRule from 'components/streamrules/HumanReadableStreamRule';
import { useStore } from 'stores/connect';
import { Icon } from 'components/common';
import { Button } from 'components/bootstrap';
import { isPermitted } from 'util/PermissionsMixin';
import StreamRuleModal from 'components/streamrules/StreamRuleModal';
import UserNotification from 'util/UserNotification';
import { StreamRulesInputsActions, StreamRulesInputsStore } from 'stores/inputs/StreamRulesInputsStore';
import { StreamRulesStore } from 'stores/streams/StreamRulesStore';
import type { StreamRule as StreamRuleTypeDefinition, Stream, StreamRule } from 'stores/streams/StreamsStore';

import useCurrentUser from '../../hooks/useCurrentUser';

const ActionButtonsWrap = styled.span`
  margin-right: 6px;
  float: right;
`;

type Props = {
  matchData?: {
    matches: boolean,
    rules: { [id: string]: false },
  },
  stream: Stream,
  onDelete: (ruleId: string) => void,
  onSubmit: (ruleId: string, data: unknown) => void,
  streamRule: StreamRuleTypeDefinition
}

const DetailsStreamRule = ({ stream, streamRule, onSubmit, onDelete }: Props) => {
  const { permissions } = useCurrentUser();
  const [showStreamRuleForm, setShowStreamRuleForm] = useState(false);
  const { inputs } = useStore(StreamRulesInputsStore);

  useEffect(() => {
    StreamRulesInputsActions.list();
  }, []);

  const _onEdit = (event: React.SyntheticEvent ) => {
    event.preventDefault();
    setShowStreamRuleForm(true);
  };

  const _onDelete = (event: React.SyntheticEvent) => {
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

  const _onSubmit = (streamRuleId: string, data: StreamRule) => StreamRulesStore.update(stream.id, streamRuleId, data, () => {
    if (onSubmit) {
      onSubmit(streamRuleId, data);
    }

    UserNotification.success('Stream rule has been successfully updated.', 'Success');
  });

  const _formatActionItems = () => (
    <ActionButtonsWrap className='align-right'>
      <Button bsStyle="link"
              bsSize="xsmall"
              onClick={_onDelete}
              title="Delete stream rule">
        <Icon name="delete" type="regular" />
      </Button>
      <Button bsStyle="link"
              bsSize="xsmall"
              onClick={_onEdit}
              title="Edit stream rule">
        <Icon name="edit_square" />
      </Button>
    </ActionButtonsWrap>
  );

    /* const matchDataStyle = () => (matchData.rules[streamRule.id] ? 'success' : 'danger'); */
  const actionItems = isPermitted(permissions, [`streams:edit:${stream.id}`]) ? _formatActionItems() : null;
  const description = streamRule.description ? <small>{' '}({streamRule.description})</small> : null;
    /* const listGroupStyle = !isEmpty(matchData) ? matchDataStyle() : null; */

  return (
    <tr key={streamRule.id}>
      <td>
        <HumanReadableStreamRule streamRule={streamRule} inputs={inputs} />
      </td>
      <td>{actionItems}</td>
      {showStreamRuleForm && (
        <StreamRuleModal initialValues={streamRule}
                         onClose={() => setShowStreamRuleForm(false)}
                         title="Edit Stream Rule"
                         submitButtonText="Update Rule"
                         submitLoadingText="Updating Rule..."
                         onSubmit={_onSubmit} />
      )}
      {description}
    </tr>
  );
};

DetailsStreamRule.propTypes = {
  matchData: PropTypes.shape({
    matches: PropTypes.bool,
    rules: PropTypes.object,
  }),
  onDelete: PropTypes.func,
  onSubmit: PropTypes.func,
  stream: PropTypes.object.isRequired,
  streamRule: PropTypes.object.isRequired,
};

DetailsStreamRule.defaultProps = {
  matchData: {},
  onSubmit: () => {},
  onDelete: () => {},
};

export default DetailsStreamRule;
