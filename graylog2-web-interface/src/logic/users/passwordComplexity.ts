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
import type { PasswordComplexityConfigType } from 'stores/configurations/ConfigurationsStore';

export const PASSWORD_SPECIAL_CHARACTERS = '!@#$%^&*()-_=+[]{}|;:,.<>?/';

export const DEFAULT_PASSWORD_COMPLEXITY_CONFIG: PasswordComplexityConfigType = {
  min_length: 6,
  require_uppercase: false,
  require_lowercase: false,
  require_numbers: false,
  require_special_chars: false,
};

const containsSpecialCharacter = (password: string) =>
  Array.from(PASSWORD_SPECIAL_CHARACTERS).some((character) => password.includes(character));

const passwordComplexityRuleMessages = (config: PasswordComplexityConfigType) => ({
  min_length: `Password must be at least ${config.min_length} characters long.`,
  require_uppercase: 'Password must contain at least one uppercase letter.',
  require_lowercase: 'Password must contain at least one lowercase letter.',
  require_numbers: 'Password must contain at least one number.',
  require_special_chars: `Password must contain at least one special character from: ${PASSWORD_SPECIAL_CHARACTERS}`,
});

export const passwordComplexityHelpLines = (config: PasswordComplexityConfigType): string[] => {
  const messages = passwordComplexityRuleMessages(config);
  const requirements = [messages.min_length];

  if (config.require_uppercase) requirements.push(messages.require_uppercase);
  if (config.require_lowercase) requirements.push(messages.require_lowercase);
  if (config.require_numbers) requirements.push(messages.require_numbers);
  if (config.require_special_chars) requirements.push(messages.require_special_chars);

  return requirements;
};

export const passwordComplexityErrors = (
  password: string,
  config: PasswordComplexityConfigType = DEFAULT_PASSWORD_COMPLEXITY_CONFIG,
): string[] => {
  if (!password) {
    return [];
  }

  const errors: string[] = [];
  const messages = passwordComplexityRuleMessages(config);

  if (password.length < config.min_length) {
    errors.push(messages.min_length);
  }

  if (config.require_uppercase && !/[A-Z]/.test(password)) {
    errors.push(messages.require_uppercase);
  }

  if (config.require_lowercase && !/[a-z]/.test(password)) {
    errors.push(messages.require_lowercase);
  }

  if (config.require_numbers && !/\d/.test(password)) {
    errors.push(messages.require_numbers);
  }

  if (config.require_special_chars && !containsSpecialCharacter(password)) {
    errors.push(messages.require_special_chars);
  }

  return errors;
};
