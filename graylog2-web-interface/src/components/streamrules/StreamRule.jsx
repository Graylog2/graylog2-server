import PropTypes from 'prop-types';
import React from 'react';

import createReactClass from 'create-react-class';

import PermissionsMixin from 'util/PermissionsMixin';
import StreamRuleForm from 'components/streamrules/StreamRuleForm';
import HumanReadableStreamRule from 'components/streamrules//HumanReadableStreamRule';

import StoreProvider from 'injection/StoreProvider';
const StreamRulesStore = StoreProvider.getStore('StreamRules');

import UserNotification from 'util/UserNotification';

const StreamRule = createReactClass({
  displayName: 'StreamRule',

  propTypes: {
    matchData: PropTypes.array,
    onDelete: PropTypes.func,
    onSubmit: PropTypes.func,
    permissions: PropTypes.array.isRequired,
    stream: PropTypes.object.isRequired,
    streamRule: PropTypes.object.isRequired,
    streamRuleTypes: PropTypes.array.isRequired,
  },

  mixins: [PermissionsMixin],

  _onEdit(event) {
    event.preventDefault();
    this.streamRuleForm.open();
  },

  _onDelete(event) {
    event.preventDefault();
    if (window.confirm('Do you really want to delete this stream rule?')) {
      StreamRulesStore.remove(this.props.stream.id, this.props.streamRule.id, () => {
        if (this.props.onDelete) {
          this.props.onDelete(this.props.streamRule.id);
        }
        UserNotification.success('Stream rule has been successfully deleted.', 'Success');
      });
    }
  },

  _onSubmit(streamRuleId, data) {
    StreamRulesStore.update(this.props.stream.id, streamRuleId, data, () => {
      if (this.props.onSubmit) {
        this.props.onSubmit(streamRuleId, data);
      }
      UserNotification.success('Stream rule has been successfully updated.', 'Success');
    });
  },

  _formatActionItems() {
    return (
      <span>
        <a href="#" onClick={this._onDelete} style={{ marginRight: 5 }}>
          <i className="fa fa-trash-o" />
        </a>
        <a href="#" onClick={this._onEdit} style={{ marginRight: 5 }}>
          <i className="fa fa-edit" />
        </a>
      </span>
    );
  },

  _getMatchDataClassNames() {
    return (this.props.matchData.rules[this.props.streamRule.id] ? 'alert-success' : 'alert-danger');
  },

  render() {
    const streamRule = this.props.streamRule;
    const streamRuleTypes = this.props.streamRuleTypes;
    const actionItems = (this.isPermitted(this.props.permissions, [`streams:edit:${this.props.stream.id}`]) ? this._formatActionItems() : null);
    const className = (this.props.matchData ? this._getMatchDataClassNames() : null);
    const description = this.props.streamRule.description ? <small>{' '}({this.props.streamRule.description})</small> : null;
    return (
      <li className={className}>
        {actionItems}
        <HumanReadableStreamRule streamRule={streamRule} streamRuleTypes={streamRuleTypes} />
        <StreamRuleForm ref={(streamRuleForm) => { this.streamRuleForm = streamRuleForm; }}
                        streamRule={streamRule}
                        streamRuleTypes={streamRuleTypes}
                        title="Edit Stream Rule"
                        onSubmit={this._onSubmit} />
        {description}
      </li>
    );
  },
});

export default StreamRule;
