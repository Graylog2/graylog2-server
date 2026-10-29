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
import { useTranslation } from 'react-i18next';

import { LinkContainer, IfPermitted } from 'components/common';
import { ButtonGroup, DropdownButton, MenuItem } from 'components/bootstrap';
import Routes from 'routing/Routes';
import HideOnCloud from 'util/conditional/HideOnCloud';

type Props = {
  node: any;
};

const NodeMaintenanceDropdown = ({ node }: Props) => {
  const { t } = useTranslation('system');

  return (
    <ButtonGroup>
      <DropdownButton
        bsStyle="info"
        bsSize="lg"
        title={t('node_maintenance_dropdown.actions')}
        id="node-maintenance-actions"
        pullRight>
        <IfPermitted permissions="threads:dump">
          <LinkContainer to={Routes.SYSTEM.THREADDUMP(node.node_id)}>
            <MenuItem>{t('node_actions.thread_dump')}</MenuItem>
          </LinkContainer>
        </IfPermitted>

        <IfPermitted permissions="processbuffer:dump">
          <LinkContainer to={Routes.SYSTEM.PROCESSBUFFERDUMP(node.node_id)}>
            <MenuItem>{t('node_actions.process_buffer_dump')}</MenuItem>
          </LinkContainer>
        </IfPermitted>

        <LinkContainer to={Routes.SYSTEM.METRICS(node.node_id)}>
          <MenuItem>{t('node_actions.metrics')}</MenuItem>
        </LinkContainer>

        <HideOnCloud>
          <IfPermitted permissions="loggers:read">
            <LinkContainer to={Routes.SYSTEM.LOGGING}>
              <MenuItem>{t('node_maintenance_dropdown.configure_internal_logging')}</MenuItem>
            </LinkContainer>
          </IfPermitted>
        </HideOnCloud>
      </DropdownButton>
    </ButtonGroup>
  );
};

export default NodeMaintenanceDropdown;
