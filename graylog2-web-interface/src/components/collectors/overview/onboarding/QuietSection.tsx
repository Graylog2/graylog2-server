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

import { Section } from 'components/common';

/**
 * A `Section` that sits flush with the page instead of in a tinted well.
 *
 * `Section` only ships a "filled" variant, and its `section.filled.background` token is lighter
 * than the page background in the light theme but darker than it in the dark theme — so a filled
 * section reads as sunken there. On the onboarding page the timeline is meant to lead, so the
 * supporting detail sections keep the section chrome (heading, radius, padding) while dropping the
 * fill and softening the border.
 */
const QuietSection = styled(Section)(
  ({ theme }) => css`
    background-color: ${theme.colors.global.contentBackground};
    border-color: ${theme.colors.cards.border};

    /* Section renders its header (the Container's first child) flush against the content;
       without the fill separating them the title needs some air below. */
    > div:first-child {
      margin-bottom: ${theme.spacings.sm};
    }
  `,
);

export default QuietSection;
