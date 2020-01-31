import React, { useRef } from 'react';
import PropTypes from 'prop-types';
import { isEmpty } from 'lodash';
import styled from 'styled-components';

import { Icon } from 'components/common';
import { Button, ListGroupItem } from 'components/graylog';
import PermissionsMixin from 'util/PermissionsMixin';
import StreamRuleForm from 'components/streamrules/StreamRuleForm';
import HumanReadableStreamRule from 'components/streamrules/HumanReadableStreamRule';

import StoreProvider from 'injection/StoreProvider';

import UserNotification from 'util/UserNotification';

const StreamRulesStore = StoreProvider.getStore('StreamRules');
const { isPermitted } = PermissionsMixin;

const ActionButtonsWrap = styled.span`
  margin-right: 6px;
`;

const StreamRule = ({ matchData, permissions, stream, streamRule, streamRuleTypes, onSubmit, onDelete }) => {
  const streamRuleFormRef = useRef();

  const _onEdit = (event) => {
    event.preventDefault();
    streamRuleFormRef.current.open();
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
          <Icon name="trash-o" />
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
      <HumanReadableStreamRule streamRule={streamRule} streamRuleTypes={streamRuleTypes} />
      <StreamRuleForm ref={streamRuleFormRef}
                      streamRule={streamRule}
                      streamRuleTypes={streamRuleTypes}
                      title="Edit Stream Rule"
                      onSubmit={_onSubmit} />
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
