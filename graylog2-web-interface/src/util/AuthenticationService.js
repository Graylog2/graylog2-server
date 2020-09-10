// @flow strict
import { PluginStore } from 'graylog-web-plugin/plugin';

// eslint-disable-next-line import/prefer-default-export
export const getAuthServicePlugin = (type: string, throwError?: boolean = false) => {
  const authServices = PluginStore.exports('authenticationServices') || [];
  const authService = authServices.find((service) => service.name === type);

  if (!authService && throwError) {
    throw new Error(`Authentication service with type "${type}" not found.`);
  }

  return authService;
};
