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
import type { ColorScheme, ColorVariant, CustomColors } from '@graylog/sawmill';

import type { LegacyColorScheme } from 'theme/constants';
import type { CustomThemesColors } from 'theme/theme-types';

interface CustomizationHooks {
  useThemeCustomizer: () => {
    currentColors: {};
    isDefaultColors: boolean;
    isSaved: boolean;
    isLoadingCustomColors: boolean;
    onChangeTheme: ({ mode, key, type, hex }: { mode: ColorScheme; key: string; type: string; hex: string }) => void;
    onResetTheme: () => Promise<unknown>;
    onRevertTheme: () => Promise<unknown>;
    onSaveTheme: () => Promise<unknown>;
  };
  useCustomThemeColors: (isInitialLoad?: boolean) => { data: CustomThemesColors; isInitialLoading: boolean };
}

interface CustomizationActions {
  generateCustomThemeColors: ({
    graylogColors,
    mode,
    initialLoad,
  }: {
    graylogColors: CustomColors;
    mode: LegacyColorScheme;
    initialLoad: boolean;
  }) => Promise<unknown>;
}

interface CustomizationType {
  hooks?: CustomizationHooks;
  actions?: CustomizationActions;
}

export interface Notification {
  title: string;
  shortMessage: string;
  longMessage: string;
  isActive: boolean;
  isDismissible: boolean;
  atLogin: boolean;
  isGlobal: boolean;
  variant: ColorVariant;
  hiddenTitle: boolean;
}
type NotificationId = string;
export type Notifications = { [id: string]: Notification };

export interface PublicNotificationsHooks {
  usePublicNotifications: () => {
    notifications: Notifications;
    dismissedNotifications: Set<NotificationId>;
    onDismissPublicNotification: (id: NotificationId) => void;
  };
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
