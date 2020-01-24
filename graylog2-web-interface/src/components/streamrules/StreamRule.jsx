import React, { useRef } from 'react';
import PropTypes from 'prop-types';

import { Icon } from 'components/common';
import { Button } from 'components/graylog';
import PermissionsMixin from 'util/PermissionsMixin';
import StreamRuleForm from 'components/streamrules/StreamRuleForm';
import HumanReadableStreamRule from 'components/streamrules//HumanReadableStreamRule';

import StoreProvider from 'injection/StoreProvider';

import UserNotification from 'util/UserNotification';

const StreamRulesStore = StoreProvider.getStore('StreamRules');
const { isPermitted } = PermissionsMixin;

const StreamRule = ({ permissions, stream, streamRule, streamRuleTypes, onSubmit, onDelete }) => {
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
      <span>
        <Button bsStyle="link"
                onClick={_onDelete}
                style={{ marginRight: 5, padding: 5 }}>
          <Icon name="trash-o" />
        </Button>
        <Button bsStyle="link"
                onClick={_onEdit}
                style={{ marginRight: 5, padding: 5 }}>
          <Icon name="edit" />
        </Button>
      </span>
    );
  };

  const actionItems = isPermitted(permissions, [`streams:edit:${stream.id}`]) ? _formatActionItems() : null;
  const description = streamRule.description ? <small>{' '}({streamRule.description})</small> : null;

  return (
    <li>
      {actionItems}
      <HumanReadableStreamRule streamRule={streamRule} streamRuleTypes={streamRuleTypes} />
      <StreamRuleForm ref={streamRuleFormRef}
                      streamRule={streamRule}
                      streamRuleTypes={streamRuleTypes}
                      title="Edit Stream Rule"
                      onSubmit={_onSubmit} />
      {description}
    </li>
  );
};

StreamRule.propTypes = {
  onDelete: PropTypes.func,
  onSubmit: PropTypes.func,
  permissions: PropTypes.array.isRequired,
  stream: PropTypes.object.isRequired,
  streamRule: PropTypes.object.isRequired,
  streamRuleTypes: PropTypes.array.isRequired,
};

StreamRule.defaultProps = {
  onSubmit: () => {},
  onDelete: () => {},
};

export default StreamRule;
