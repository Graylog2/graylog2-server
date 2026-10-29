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
import { withTranslation } from 'react-i18next';
import type { WithTranslation } from 'react-i18next';

import ButtonToolbar from 'components/bootstrap/ButtonToolbar';
import { LinkContainer, DropdownSubmenu, IfPermitted } from 'components/common';
import { Button, DropdownButton, MenuItem } from 'components/bootstrap';
import Routes from 'routing/Routes';
import HideOnCloud from 'util/conditional/HideOnCloud';
import { overrideLoadBalancerStatus } from 'api/system-load-balancer';
import { pauseProcessing, resumeProcessing } from 'api/system-processing';

type NodesActionsProps = {
  node: any;
  systemOverview: any;
} & WithTranslation;

class NodesActions extends React.Component<
  NodesActionsProps,
  {
    [key: string]: any;
  }
> {
  _toggleMessageProcessing = () => {
    const { systemOverview, node, t } = this.props;

    if (
      // eslint-disable-next-line no-alert
      window.confirm(
        systemOverview.is_processing ? t('nodes_actions.pause_confirm') : t('nodes_actions.resume_confirm'),
      )
    ) {
      if (systemOverview.is_processing) {
        pauseProcessing(node.node_id);
      } else {
        resumeProcessing(node.node_id);
      }
    }
  };

  _changeLBStatus = (status) => () => {
    const { t } = this.props;

    // eslint-disable-next-line no-alert
    if (window.confirm(t('nodes_actions.load_balancer_confirm', { status }))) {
      const { node } = this.props;
      overrideLoadBalancerStatus(node.node_id, status);
    }
  };

  render() {
    const { systemOverview, node, t } = this.props;

    return (
      <ButtonToolbar>
        <LinkContainer to={Routes.SYSTEM.CLUSTER.NODE_SHOW(node.node_id)}>
          <Button>{t('nodes_actions.details')}</Button>
        </LinkContainer>

        <LinkContainer to={Routes.SYSTEM.METRICS(node.node_id)}>
          <Button>{t('nodes_actions.metrics')}</Button>
        </LinkContainer>

        <DropdownButton
          title={t('nodes_actions.more_actions')}
          id={`more-actions-dropdown-${node.node_id}`}
          pullRight>
          <IfPermitted permissions="processing:changestate">
            <MenuItem onSelect={this._toggleMessageProcessing}>
              {systemOverview.is_processing
                ? t('nodes_actions.pause_message_processing')
                : t('nodes_actions.resume_message_processing')}
            </MenuItem>
          </IfPermitted>

          <IfPermitted permissions="lbstatus:change">
            <DropdownSubmenu title={t('nodes_actions.override_lb_status')} left>
              <MenuItem onSelect={this._changeLBStatus('ALIVE')}>{t('nodes_actions.status_alive')}</MenuItem>
              <MenuItem onSelect={this._changeLBStatus('DEAD')}>{t('nodes_actions.status_dead')}</MenuItem>
            </DropdownSubmenu>
          </IfPermitted>

          <IfPermitted permissions={['processing:changestate', 'lbstatus:change', 'node:shutdown']} anyPermissions>
            <IfPermitted permissions={['inputs:read', 'threads:dump']} anyPermissions>
              <MenuItem divider />
            </IfPermitted>
          </IfPermitted>

          <HideOnCloud>
            <IfPermitted permissions="inputs:read">
              <LinkContainer to={Routes.node_inputs(node.node_id)}>
                <MenuItem>{t('nodes_actions.local_message_inputs')}</MenuItem>
              </LinkContainer>
            </IfPermitted>
          </HideOnCloud>
          <IfPermitted permissions="threads:dump">
            <LinkContainer to={Routes.SYSTEM.THREADDUMP(node.node_id)}>
              <MenuItem>{t('nodes_actions.thread_dump')}</MenuItem>
            </LinkContainer>
          </IfPermitted>
          <IfPermitted permissions="processbuffer:dump">
            <LinkContainer to={Routes.SYSTEM.PROCESSBUFFERDUMP(node.node_id)}>
              <MenuItem>{t('nodes_actions.process_buffer_dump')}</MenuItem>
            </LinkContainer>
          </IfPermitted>
          <IfPermitted permissions="loggersmessages:read">
            <LinkContainer to={Routes.SYSTEM.SYSTEMLOGS(node.node_id)}>
              <MenuItem>{t('nodes_actions.recent_system_logs')}</MenuItem>
            </LinkContainer>
          </IfPermitted>
        </DropdownButton>
      </ButtonToolbar>
    );
  }
}

export default withTranslation('system')(NodesActions);
