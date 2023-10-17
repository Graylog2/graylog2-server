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
import { useCallback } from 'react';

import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import OnSaveViewAction from 'views/logic/views/OnSaveViewAction';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useAppDispatch from 'stores/useAppDispatch';
import SaveViewButton from 'views/components/searchbar/SaveViewButton';
import useHotkey from 'hooks/useHotkey';
import useView from 'views/hooks/useView';
import useIsNew from 'views/hooks/useIsNew';
import useHasUndeclaredParameters from 'views/logic/parameters/useHasUndeclaredParameters';

type Props = {
  userIsAllowedToEdit: boolean,
  openSaveAsModal: () => void,
}

const SaveDashboardButton = ({ userIsAllowedToEdit, openSaveAsModal }: Props) => {
  const view = useView();
  const isNewView = useIsNew();
  const sendTelemetry = useSendTelemetry();
  const dispatch = useAppDispatch();
  const hasUndeclaredParameters = useHasUndeclaredParameters();
  const _onSaveView = useCallback(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.DASHBOARD_ACTION.DASHBOARD_SAVED, {
      app_pathname: 'dashboard',
      app_section: 'dashboard',
      app_action_value: 'dashboard-save',
    });

    return dispatch(OnSaveViewAction(view));
  }, [dispatch, sendTelemetry, view]);

  useHotkey({
    actionKey: 'save',
    callback: () => (isNewView ? openSaveAsModal() : _onSaveView()),
    scope: 'dashboard',
    options: { enabled: !hasUndeclaredParameters && userIsAllowedToEdit },
  });

  return (
    <SaveViewButton title="Save dashboard"
                    onClick={_onSaveView}
                    disabled={hasUndeclaredParameters || isNewView || !userIsAllowedToEdit} />
  );
};

export default SaveDashboardButton;
