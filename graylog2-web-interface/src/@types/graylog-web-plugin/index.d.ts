interface PluginRoute {
  path: string;
  component: React.ComponentType;
  parentComponent?: React.ComponentType | null;
  permissions?: string;
}
interface PluginNavigation {
  path: string;
  description: string;
}
interface PluginNavigationItems {
  key: string;
  component: React.ComponentType<{ smallScreen?: boolean }>;
}
interface GlobalNotification {
  key: string;
  component: React.ComponentType;
}

interface PluginPages {
  search?: {
    component: React.ComponentType;
  }
}

declare module 'graylog-web-plugin/plugin' {
  interface PluginExports {
    navigation?: Array<PluginNavigation>;
    navigationItems?: Array<PluginNavigationItems>;
    globalNotifications?: Array<GlobalNotification>
    routes?: Array<PluginRoute>;
    pages?: PluginPages;
  }

  interface PluginRegistration {
    exports: PluginExports;
  }

  interface PluginManifest extends PluginRegistration {
    new (json: {}, exports: PluginExports): PluginManifest;
  }

  interface PluginStore {
    register: (manifest: PluginRegistration) => void;
    exports: <T extends keyof PluginExports>(key: T) => PluginExports[T];
    unregister: (manifest: PluginRegistration) => void;
  }

  const PluginStore: PluginStore;
  const PluginManifest: PluginManifest;
}
