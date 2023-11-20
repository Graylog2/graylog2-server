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
export type HttpEventNotificationV2 = EventNotification & {
  config: HttpNotificationConfigV2,
};

export type EncryptedValue = {
  is_set?: boolean,
  set_value?: string,
  keep_value?: boolean,
  delete_value?: boolean
};

export type HttpNotificationConfigV2 = {
  type: 'http-notification-v2',
  url: string,
  basic_auth?: EncryptedValue,
  api_key_as_header: boolean,
  api_key?: string,
  api_secret?: EncryptedValue,
  skip_tls_verification: boolean,
  time_zone?: string,
  method: string,
  content_type?: string,
  body_template?: string,
  headers?: string,
};

export type HttpNotificationValidationV2 = {
  failed?: boolean,
  errors?: ErrorType,
}

interface EventNotificationTypes {
  type: string,
  displayName: string,
  formComponent: React.ComponentType<React.ComponentProps<{
    config: EventNotification['config'],
    validation: { errors: { [key: string]: Array<string> } },
    onChange: (newConfig: EventNotification['config']) => void,
  }>>,
  summaryComponent: React.ComponentType<React.ComponentProps<{
    config: EventNotification['config'],
    validation: { errors: { [key: string]: Array<string> } },
    onChange: (newConfig: EventNotification['config']) => void,
  }>>,
  detailsComponent: React.ComponentType<React.ComponentProps<{
    config: EventNotification['config'],
    validation: { errors: { [key: string]: Array<string> } },
    onChange: (newConfig: EventNotification['config']) => void,
  }>>,
  defaultConfig: EventNotification['config'],
}

declare module 'graylog-web-plugin/plugin' {
  interface PluginExports {
    'eventNotificationTypes'?: Array<EventNotificationTypes>;
  }
}
