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
import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { LinkContainer } from 'components/graylog/router';
import { DropdownButton, MenuItem, Button } from 'components/graylog';
import { AlertConditionForm, AlertConditionSummary, AlertConditionTestModal, UnknownAlertCondition } from 'components/alertconditions';
import PermissionsMixin from 'util/PermissionsMixin';
import CombinedProvider from 'injection/CombinedProvider';
import Routes from 'routing/Routes';

const { AlertConditionsActions } = CombinedProvider.get('AlertConditions');
const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

const AlertCondition = createReactClass({
  displayName: 'AlertCondition',

  propTypes: {
    alertCondition: PropTypes.object.isRequired,
    conditionType: PropTypes.object.isRequired,
    stream: PropTypes.object.isRequired,
    isDetailsView: PropTypes.bool,
    isStreamView: PropTypes.bool,
    onUpdate: PropTypes.func,
    onDelete: PropTypes.func,
  },
  mixins: [Reflux.connect(CurrentUserStore), PermissionsMixin],

  getDefaultProps() {
    return {
      isDetailsView: false,
      isStreamView: false,
      onUpdate: () => {},
      onDelete: () => {},
    };
  },

  _onEdit() {
    this.updateForm.open();
  },

  _onUpdate(request) {
    const { stream, alertCondition, onUpdate } = this.props;

    AlertConditionsActions.update(stream.id, alertCondition.id, request)
      .then(() => onUpdate(stream.id, alertCondition.id));
  },

  _onDelete() {
    const { stream, alertCondition, onDelete } = this.props;

    // eslint-disable-next-line no-alert
    if (window.confirm('Really delete alert condition?')) {
      AlertConditionsActions.delete(stream.id, alertCondition.id)
        .then(() => onDelete(stream.id, alertCondition.id));
    }
  },

  _openTestModal() {
    this.modal.open();
  },

  render() {
    const { stream, alertCondition, conditionType, isDetailsView, isStreamView } = this.props;
    const { currentUser: { permissions } } = this.state;

    if (!conditionType) {
      return <UnknownAlertCondition alertCondition={alertCondition} onDelete={this._onDelete} stream={stream} />;
    }

    let actions = [];

    if (this.isPermitted(permissions, `streams:edit:${stream.id}`)) {
      actions = [
        <Button key="test-button" bsStyle="info" onClick={this._openTestModal}>Test</Button>,
        <DropdownButton key="more-actions-button"
                        title="More actions"
                        pullRight
                        id={`more-actions-dropdown-${alertCondition.id}`}>
          {!isStreamView && (
            <LinkContainer to={Routes.stream_alerts(stream.id)}>
              <MenuItem>Alerting overview for Stream</MenuItem>
            </LinkContainer>
          )}
          <MenuItem onSelect={this._onEdit}>Edit</MenuItem>
          <MenuItem divider />
          <MenuItem onSelect={this._onDelete}>Delete</MenuItem>
        </DropdownButton>,
      ];
    }

    return (
      <>
        <AlertConditionForm ref={(updateForm) => { this.updateForm = updateForm; }}
                            conditionType={conditionType}
                            alertCondition={alertCondition}
                            onSubmit={this._onUpdate} />
        <AlertConditionTestModal ref={(c) => { this.modal = c; }}
                                 stream={stream}
                                 condition={alertCondition} />
        <AlertConditionSummary alertCondition={alertCondition}
                               conditionType={conditionType}
                               stream={stream}
                               actions={actions}
                               isDetailsView={isDetailsView} />
      </>
    );
  },
});

export default AlertCondition;
