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
export type TeamsNotificationSummaryType = {
    type: string,
    notification: NotificationType,
}
export type TeamsNotificationSummaryV2Type = {
    type: string,
    notification: NotificationV2Type,
}

export type NotificationType = {
    config: ConfigType,
}

export type NotificationV2Type = {
    config: ConfigV2Type,
}

export interface ConfigType {
    defaultValue?: any,
    icon_url?: string,
    backlog_size?: number,
    custom_message: string,
    webhook_url?: string,
    color?: string,
    time_zone?: string,
}

export interface ConfigV2Type {
  backlog_size?: number,
  adaptive_card: string,
  webhook_url?: string,
  time_zone?: string,
}

export type ValidationType = {
    failed?: boolean,
    errors?: ErrorType,
}

export interface ErrorType {
  webhook_url: string[],
  color: string[],
  icon_url: string,
  backlog_size: number,
  custom_message: string,
  adaptive_card: string,
  url: string,
  api_key: string,
  api_secret: string,
  headers: string[],
  body_template: string,
}

export type SlackNotificationSummaryType = {
    type: string,
    notification: SlackNotificationType,
    definitionNotification: any,
}

export type SlackNotificationType = {
    config: SlackConfigType,
}

export interface SlackConfigType {
    icon_emoji?: string,
    icon_url?: string,
    link_names: string,
    notify_channel: boolean,
    notify_here: boolean,
    backlog_size: number,
    user_name?: string,
    custom_message: string,
    channel: string,
    webhook_url: string,
    color: string,
    time_zone: string,
    include_title: boolean,
}

export type SlackValidationType = {
    failed: boolean,
    errors?: SlackErrorType,
    error_context?: any
}

export interface SlackErrorType {
    icon_emoji?: string,
    icon_url?: string,
    link_names: string,
    notify_channel: string,
    notify_here: string,
    backlog_size: number,
    user_name?: string,
    custom_message: string,
    channel: string,
    webhook_url: string,
    color: string,
    time_zone: string,
    include_title?: string,
}
