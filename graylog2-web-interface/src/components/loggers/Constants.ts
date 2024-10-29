export const availableLoglevels = [
  'fatal',
  'error',
  'warn',
  'info',
  'debug',
  'trace',
] as const;

export type AvailableLogLevels = typeof availableLoglevels;
