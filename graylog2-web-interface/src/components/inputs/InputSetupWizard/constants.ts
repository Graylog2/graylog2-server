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
import { INPUT_WIZARD_STEPS, INPUT_WIZARD_FLOWS } from 'components/inputs/InputSetupWizard/types';

export const INPUT_SETUP_MODE_FEATURE_FLAG = 'setup_mode';

export default INPUT_SETUP_MODE_FEATURE_FLAG;

export const OPEN_FLOW_STEPS = {
  [INPUT_WIZARD_FLOWS.ILLUMINATE]: [INPUT_WIZARD_STEPS.START_INPUT, INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS],
  [INPUT_WIZARD_FLOWS.NON_ILLUMINATE]: [
    INPUT_WIZARD_STEPS.SETUP_ROUTING,
    INPUT_WIZARD_STEPS.START_INPUT,
    INPUT_WIZARD_STEPS.INPUT_DIAGNOSIS,
  ],
};
