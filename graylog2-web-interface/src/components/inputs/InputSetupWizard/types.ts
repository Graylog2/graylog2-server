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

import type { Input } from 'components/messageloaders/Types';

export const INPUT_WIZARD_STEPS = {
  SELECT_CATEGORY: 'SELECT_CATEGORY',
  INPUT_DIAGNOSIS: 'INPUT_DIAGNOSIS',
  SETUP_ROUTING: 'SETUP_ROUTING',
  START_INPUT: 'START_INPUT',
} as const;

export const INPUT_WIZARD_CATEGORIES = {
  GENERIC: 'GENERIC',
} as const;

export type InputSetupWizardStep = typeof INPUT_WIZARD_STEPS[keyof typeof INPUT_WIZARD_STEPS]
export type InputSetupWizardCategory = typeof INPUT_WIZARD_CATEGORIES[keyof typeof INPUT_WIZARD_CATEGORIES]

export type StepConfig = {
  enabled?: boolean
}

export type StepsConfig = {
  [key in InputSetupWizardStep]?: StepConfig
}

export type StepsData = {
  [key in InputSetupWizardStep]?: object
}

export type WizardData = {
  input?: Input,
  category?: InputSetupWizardCategory
}
