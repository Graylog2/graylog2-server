import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import { DirectoryServiceBackendConfig } from 'logic/authentication/directoryServices/types';

interface AuthenticationService {
  name: string;
  displayName: string;
  createComponent: React.ComponentType<{}>;
  editComponent: React.ComponentType<any>;
  configDetailsComponent: React.ComponentType<any>;
  configToJson: (config: {}) => {};
  configFromJson: (json: {}) => DirectoryServiceBackendConfig;
}

interface DirectoryServicesGroupSync {
  components: {
    GroupSyncSection: React.ComponentType<{ authenticationBackend: AuthenticationBackend }>;
  }
}

interface AuthenticationPlugin {
  components: {
    SyncedTeamsSection: React.ComponentType<{ authenticationBackend: AuthenticationBackend }>;
  };
}

declare module 'graylog-web-plugin/plugin' {
  interface PluginExports {
    'authentication.services'?: Array<AuthenticationService>;
    'authentication.enterprise.directoryServices.groupSync'?: DirectoryServicesGroupSync;
    'authentication.enterprise'?: Array<AuthenticationPlugin>;
  }
}
