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
import { PluginStore } from 'graylog-web-plugin/plugin';

import AppConfig from 'util/AppConfig';
import UsersDomain from 'domainActions/users/UsersDomain';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

const isCloud = AppConfig.isCloud();
const oktaUserForm = isCloud ? PluginStore.exports('cloud')[0].oktaUserForm : null;

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type FormValues = Record<string, any>;

const useUserCreateSubmit = () => {
  const sendTelemetry = useSendTelemetry();

  return async (values: FormValues) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.USERS.USER_CREATED, {
      app_action_value: 'user-create-form',
    });

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let data: any = { ...values, permissions: [] };
    delete data.password_repeat;

    if (isCloud && oktaUserForm) {
      const { onCreate } = oktaUserForm;
      data = onCreate(data);
    } else {
      data.username = data.username.trim();
    }

    await UsersDomain.create(data);

    return UsersDomain.loadByUsername(data.username);
  };
};

export default useUserCreateSubmit;
