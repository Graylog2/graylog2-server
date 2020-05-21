import styled, { css } from 'styled-components';

import AceEditor from './ace';

const StyledAceEditor = styled(AceEditor).attrs(({ aceTheme, theme }) => ({
  // NOTE: After setting the prop we need to swap them back so AceEditor uses the proper styles
  theme: aceTheme,
  scTheme: theme,
}))(({ scTheme }) => css`
  &.ace-queryinput {
    height: 34px !important;
    width: 100% !important;
    background-color: ${scTheme.colors.global.inputBackground};
    color: ${scTheme.utils.contrastingColor(scTheme.colors.global.inputBackground, 'AAA')};


    &.ace_multiselect .ace_selection.ace_start {
      box-shadow: 0 0 3px 0 ${scTheme.colors.global.inputBackground};
    }

    .ace_gutter {
      background: ${scTheme.colors.global.inputBackground};
      color: ${scTheme.utils.contrastingColor(scTheme.colors.global.inputBackground, 'AAA')};
    }

    .ace_print-margin {
      width: 1px;
      background: ${scTheme.colors.global.inputBackground};
    }

    .ace_cursor {
      color: ${scTheme.colors.gray[50]};
    }

    .ace_marker-layer .ace_selection {
      background: ${scTheme.colors.variant.lightest.default};
    }

    .ace_marker-layer .ace_step {
      background: ${scTheme.colors.variant.warning};
    }

    .ace_marker-layer .ace_bracket {
      margin: -1px 0 0 -1px;
      border: none;
    }

    .ace_marker-layer .ace_active-line {
      background: ${scTheme.colors.global.inputBackground};
    }

    .ace_gutter-active-line {
      background-color: ${scTheme.colors.global.inputBackground};
    }

    .ace_marker-layer .ace_selected-word {
      border: 1px solid ${scTheme.colors.gray[80]};
    }

    .ace_invisible {
      color: ${scTheme.colors.global.inputBackground};
    }

    .ace_keyword,
    .ace_meta,
    .ace_storage,
    .ace_storage.ace_type,
    .ace_support.ace_type {
      color: ${scTheme.colors.variant.primary};
    }

    .ace_keyword.ace_operator {
      color: ${scTheme.colors.variant.darker.info};
    }

    .ace_constant.ace_character,
    .ace_constant.ace_language,
    .ace_constant.ace_numeric,
    .ace_keyword.ace_other.ace_unit,
    .ace_support.ace_constant,
    .ace_variable.ace_parameter {
      color: ${scTheme.colors.variant.dark.danger};
    }

    .ace_constant.ace_other {
      color: ${scTheme.colors.variant.default};
    }

    .ace_invalid {
      color: ${scTheme.utils.readableColor(scTheme.colors.brand.primary)};
      background-color: ${scTheme.colors.brand.primary};
    }

    .ace_invalid.ace_deprecated {
      color: ${scTheme.utils.readableColor(scTheme.colors.brand.primary)};
      background-color: ${scTheme.colors.variant.dark.primary};
    }

    .ace_fold {
      background-color: ${scTheme.colors.variant.info};
      border-color: ${scTheme.utils.contrastingColor(scTheme.colors.global.inputBackground, 'AAA')};
    }

    .ace_entity.ace_name.ace_function,
    .ace_support.ace_function,
    .ace_variable,
    .ace_term {
      color: ${scTheme.colors.variant.info};
    }

    .ace_support.ace_class,
    .ace_support.ace_type {
      color: ${scTheme.colors.variant.dark.warning};
    }

    .ace_heading,
    .ace_markup.ace_heading,
    .ace_string {
      color: ${scTheme.colors.variant.dark.success};
    }

    .ace_entity.ace_name.ace_tag,
    .ace_entity.ace_other.ace_attribute-name,
    .ace_meta.ace_tag,
    .ace_string.ace_regexp,
    .ace_variable {
      color: ${scTheme.colors.brand.primary};
    }

    .ace_comment {
      color: ${scTheme.colors.gray[60]};
    }

    .ace_indent-guide {
      background: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAACCAYAAACZgbYnAAAAE0lEQVQImWP4////f4bdu3f/BwAlfgctduB85QAAAABJRU5ErkJggg==) right repeat-y;
    }

    .ace_content,
    .ace_placeholder {
      top: 6px;
      font-size: 16px;
      padding: 0 !important;
      font-family: inherit !important;
    }
  }
`);

export default StyledAceEditor;
