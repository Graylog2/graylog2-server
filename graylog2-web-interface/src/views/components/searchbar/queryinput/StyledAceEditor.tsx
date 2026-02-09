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
import * as React from 'react';
import styled, { css } from 'styled-components';
import type { DefaultTheme } from 'styled-components';

import { INPUT_BORDER_RADIUS } from 'theme/constants';

import AceEditor from './ace';

type Props = {
  $height: number;
  $scTheme: DefaultTheme;
  disabled: boolean;
};

const AceEditorContainer = styled.div(
  ({ theme }) => css`
    ${theme.components.aceEditor}
  `,
);

const StyledAceEditorBase = styled(AceEditor)<Props>(
  ({ $scTheme, $height, disabled }) => css`
    &.ace-queryinput {
      ${$height ? `height: ${$height}px !important` : ''}
      min-height: 34px;
      width: 100% !important;
      --ace-text: ${$scTheme.utils.contrastingColor($scTheme.colors.input.background, 'AAA')};
      --ace-selection-start-shadow: ${$scTheme.colors.input.background};
      --ace-gutter-text: ${$scTheme.utils.contrastingColor($scTheme.colors.input.background, 'AAA')};
      --ace-token-invalid-color: ${$scTheme.utils.readableColor($scTheme.colors.variant.danger)};
      --ace-token-invalid-deprecated-color: ${$scTheme.utils.readableColor($scTheme.colors.variant.danger)};
      --ace-token-fold-border: ${$scTheme.utils.contrastingColor($scTheme.colors.input.background, 'AAA')};
      --ace-border-radius: ${INPUT_BORDER_RADIUS};
      border-radius: ${INPUT_BORDER_RADIUS};

      .ace_cursor {
        display: ${disabled ? 'none' : 'block'} !important;
      }

      .ace_indent-guide {
        background: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAACCAYAAACZgbYnAAAAE0lEQVQImWP4////f4bdu3f/BwAlfgctduB85QAAAABJRU5ErkJggg==')
          right repeat-y;
      }
    }
  `,
);

const StyledAceEditor = React.forwardRef<any, Props>((props, ref) => (
  <AceEditorContainer>
    <StyledAceEditorBase {...props} ref={ref} />
  </AceEditorContainer>
));

export default StyledAceEditor;
