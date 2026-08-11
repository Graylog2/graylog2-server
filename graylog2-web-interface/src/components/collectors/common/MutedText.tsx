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
import styled, { css } from 'styled-components';

/**
 * Muted small-print paragraph for contextual hints outside of forms (step-state notes, empty-state
 * hints, inline error notices). The collectors-wide equivalent of the ad-hoc `Note`/`StepDetail`
 * styles in the onboarding components. For help text under form controls, use `HelpBlock` instead.
 */
const MutedText = styled.p(
  ({ theme }) => css`
    color: ${theme.colors.gray[60]};
    font-size: ${theme.fonts.size.small};
    margin: ${theme.spacings.md} 0 0 0;
  `,
);

export default MutedText;
