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

export type NotificationType = {
    config: ConfigType,
}

export interface ConfigType {
    defaultValue?: any,
    graylog_url?: string,
    icon_url?: string,
    backlog_size?: number,
    custom_message: string,
    webhook_url?: string,
    color?: string,
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
}
