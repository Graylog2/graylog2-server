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
import React, { useMemo, useCallback, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';
import TemplateForm from 'components/indices/IndexSetTemplates/TemplateForm';
import type {
  IndexSetTemplate,
} from 'components/indices/IndexSetTemplates/types';
import useTemplateMutation from 'components/indices/IndexSetTemplates/hooks/useTemplateMutation';
import Routes from 'routing/Routes';
import useHistory from 'routing/useHistory';

const CreateTemplate = () => {
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const { createTemplate } = useTemplateMutation();
  const telemetryPathName = useMemo(() => getPathnameWithoutId(pathname), [pathname]);
  const history = useHistory();

  const onSubmit = useCallback((template: IndexSetTemplate) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.INDEX_SET_TEMPLATE.CREATED, {
      app_pathname: telemetryPathName,
      app_action_value: 'create-new-index-set-template-created',
    });

    createTemplate(template).then(() => {
      navigate(Routes.SYSTEM.INDICES.TEMPLATES.OVERVIEW);
    });
  }, [createTemplate, navigate, sendTelemetry, telemetryPathName]);

  useEffect(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.INDEX_SET_TEMPLATE.NEW_OPENED, { app_pathname: telemetryPathName, app_action_value: 'create-new-index-set-template-opened' });
  }, [sendTelemetry, telemetryPathName]);

  const onCancel = useCallback(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.INDEX_SET_TEMPLATE.NEW_CANCELLED, { app_pathname: telemetryPathName, app_action_value: 'create-new-index-set-template-cancelled' });
    history.goBack();
  }, [history, sendTelemetry, telemetryPathName]);

  return (
    <TemplateForm onCancel={onCancel} submitButtonText="Create template" submitLoadingText="Creating template..." onSubmit={onSubmit} />
  );
};

export default CreateTemplate;
