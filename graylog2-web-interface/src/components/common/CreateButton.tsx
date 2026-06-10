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
import React, { useCallback, useMemo } from 'react';
import type { Permissions } from 'graylog-web-plugin/plugin';

import IfPermitted from 'components/common/IfPermitted';
import { LinkContainer } from 'components/common';
import { Button } from 'components/bootstrap';
import usePluginEntities from 'hooks/usePluginEntities';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import { getPathnameWithoutId } from 'util/URLUtils';

type Props = {
  entityKey: string;
  disabled?: boolean;
};

const useEntityCreator = (entityKey: string) => {
  const entityCreators = usePluginEntities('entityCreators');

  const entityCreator = useMemo(
    () => entityCreators.find((creator) => creator.id === entityKey),
    [entityCreators, entityKey],
  );
  if (!entityCreator) {
    throw new Error(`Entity creator "${entityKey}" not found!`);
  }

  return entityCreator;
};

const PermissionWrapper = ({
  permissions = undefined,
  children = undefined,
}: React.PropsWithChildren<{ permissions?: Permissions }>) =>
  permissions ? <IfPermitted permissions={permissions}>{children}</IfPermitted> : <>{children}</>;

const CreateButton = ({ disabled = false, entityKey }: Props) => {
  const entityCreator = useEntityCreator(entityKey);
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();

  const _onClick = useCallback(() => {
    const { telemetryEvent } = entityCreator;
    if (telemetryEvent) {
      sendTelemetry(telemetryEvent.type, {
        app_pathname: getPathnameWithoutId(pathname),
        app_section: telemetryEvent.section,
        app_action_value: telemetryEvent.actionValue,
      });
    }
  }, [entityCreator, pathname, sendTelemetry]);

  return (
    <PermissionWrapper>
      <LinkContainer to={entityCreator.path}>
        <Button bsSize="md" bsStyle="primary" onClick={_onClick} disabled={disabled}>
          {entityCreator.title}
        </Button>
      </LinkContainer>
    </PermissionWrapper>
  );
};

export default CreateButton;
