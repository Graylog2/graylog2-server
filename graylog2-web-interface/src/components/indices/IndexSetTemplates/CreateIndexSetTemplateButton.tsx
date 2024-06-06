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

import * as React from 'react';
import { useMemo } from 'react';
import { useNavigate } from 'react-router-dom';

import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import { getPathnameWithoutId } from 'util/URLUtils';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { Button } from 'components/bootstrap';
import Routes from 'routing/Routes';

const CreateIndexSetTemplateButton = () => {
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();
  const telemetryPathName = useMemo(() => getPathnameWithoutId(pathname), [pathname]);
  const navigate = useNavigate();

  const handleClick = () => {
    sendTelemetry(
      TELEMETRY_EVENT_TYPE.INDEX_SET_TEMPLATE.SELECT_OPENED,
      {
        app_pathname: telemetryPathName,
        app_action_value: 'select-index-set-template-opened',
      });

    navigate(Routes.SYSTEM.INDICES.TEMPLATES.CREATE);
  };

  return (
    <Button bsStyle="success" onClick={handleClick}>Create template</Button>
  );
};

export default CreateIndexSetTemplateButton;
