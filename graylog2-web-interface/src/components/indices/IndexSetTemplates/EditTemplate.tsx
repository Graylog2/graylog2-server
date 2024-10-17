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

type Props = {
  template: IndexSetTemplate,
}

const EditTemplate = ({
  template,
}: Props) => {
  const sendTelemetry = useSendTelemetry();
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const { updateTemplate } = useTemplateMutation();
  const telemetryPathName = useMemo(() => getPathnameWithoutId(pathname), [pathname]);

  const onSubmit = useCallback((newTemplate: IndexSetTemplate) => {
    updateTemplate({ template: newTemplate, id: template.id }).then(() => {
      sendTelemetry(TELEMETRY_EVENT_TYPE.INDEX_SET_TEMPLATE.EDIT, {
        app_pathname: telemetryPathName,
        app_action_value: 'edit-index-set-template-edited',
      });

      navigate(Routes.SYSTEM.INDICES.TEMPLATES.OVERVIEW);
    });
  }, [updateTemplate, navigate, template.id, sendTelemetry, telemetryPathName]);

  useEffect(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.INDEX_SET_TEMPLATE.EDIT_OPENED, { app_pathname: telemetryPathName, app_action_value: 'edit-index-set-template-opened' });
  }, [sendTelemetry, telemetryPathName]);

  const onCancel = useCallback(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.INDEX_SET_TEMPLATE.EDIT_CANCELLED, { app_pathname: telemetryPathName, app_action_value: 'edit-index-set-template-cancelled' });
    navigate(Routes.SYSTEM.INDICES.TEMPLATES.OVERVIEW);
  }, [navigate, sendTelemetry, telemetryPathName]);

  return (
    <TemplateForm onCancel={onCancel}
                  submitButtonText="Update template"
                  submitLoadingText="Updating template..."
                  onSubmit={onSubmit}
                  initialValues={template} />
  );
};

export default EditTemplate;
