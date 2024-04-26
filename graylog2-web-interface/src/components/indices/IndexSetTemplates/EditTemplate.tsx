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
import omit from 'lodash/omit';

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
  const { editTemplate } = useTemplateMutation();
  const telemetryPathName = useMemo(() => getPathnameWithoutId(pathname), [pathname]);

  const onSubmit = useCallback((newTemplate: IndexSetTemplate) => {
    editTemplate({ template: newTemplate, id: template.id }).then(() => {
      sendTelemetry(TELEMETRY_EVENT_TYPE.INDEX_SET_TEMPLATE.EDIT, {
        app_pathname: telemetryPathName,
        app_action_value: 'edit-new-index-set-template-edited', // TODO: add template name
      });

      navigate(Routes.SYSTEM.INDICES.TEMPLATES.OVERVIEW);
    });
  }, [editTemplate, navigate, template.id, sendTelemetry, telemetryPathName]);

  useEffect(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.INDEX_SET_TEMPLATE.EDIT_OPENED, { app_pathname: telemetryPathName, app_action_value: 'edit-new-index-set-template-opened' });
  }, [sendTelemetry, telemetryPathName]);

  const onCancel = useCallback(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.INDEX_SET_TEMPLATE.EDIT_CANCELED, { app_pathname: telemetryPathName, app_action_value: 'edit-new-index-set-template-cancelled' });
    navigate(Routes.SYSTEM.INDICES.TEMPLATES.OVERVIEW);
  }, [navigate, sendTelemetry, telemetryPathName]);

  const initialValues = useMemo(() => omit(template, ['id']), [template]);

  return (
    <TemplateForm onCancel={onCancel} submitButtonText="Update template" submitLoadingText="Updating template..." onSubmit={onSubmit} initialValues={initialValues} />
  );
};

export default EditTemplate;
