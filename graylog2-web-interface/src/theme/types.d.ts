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
import { ThemeMode } from 'theme/constants';
import { Colors, ColorVariants, ThemeColorModes } from 'theme/colors';

interface CustomizationHooks {
  useThemeCustomizer: () => ({
    currentColors: ThemeColorModes,
    customThemeColors: {},
    isDefaultColors: boolean,
    isSaved: boolean,
    onChangeTheme: ({ mode, key, type, hex }: {mode:ThemeMode, key:string, type:string, hex:string}) => void,
    onResetTheme: () => Promise,
    onRevertTheme: () => Promise,
    onSaveTheme: () => Promise,
  })
}

interface CustomizationActions {
  generateCustomThemeColors: ({ graylogColors, mode, initialLoad }: {graylogColors: Colors, mode: ThemeMode, initialLoad: boolean}) => Promise,
}

interface CustomizationType {
  hooks?: CustomizationHooks;
  actions?: CustomizationActions;
}

export interface Notification {
  title: string,
  shortMessage: string,
  longMessage: string,
  isActive: boolean,
  isDismissible: boolean,
  atLogin: boolean,
  isGlobal: boolean,
  variant: ColorVariants,
  hiddenTitle: boolean,
}
export type Notifications = Array<Notification>;
type NotificationId = string;

export interface PublicNotificationsHooks {
  usePublicNotifications: () => ({
    notifications: Notifications,
    dismissedNotifications: Set<NotificationId>,
    onDismissPublicNotification: (NotificationId) => void,
  })
}

interface PublicNotificationsType {
  hooks?: PublicNotificationsHooks;
}

declare module 'graylog-web-plugin/plugin' {
  interface PluginExports {
    'customization.theme.customizer'?: Array<CustomizationType>;
    'customization.publicNotifications'?: Array<PublicNotificationsType>;
  }
}
