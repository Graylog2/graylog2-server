import styled, { css } from 'styled-components';

import { util } from 'theme';
import AceEditor from './ace';

const StyledAceEditor = styled(AceEditor).attrs(({ aceTheme, theme }) => ({
  // NOTE: After setting the prop we need to swap them back so AceEditor uses the proper styles
  theme: aceTheme,
  scTheme: theme,
}))(({ scTheme }) => css`
  height: 34px !important;
  line-height: 32px;
  width: 100% !important;

  &.ace-queryinput .ace_gutter {
    background: ${scTheme.color.global.inputBackground};
    color: ${util.contrastingColor(scTheme.color.global.inputBackground, 'AAA')};
  }

  &.ace-queryinput .ace_print-margin {
    width: 1px;
    background: ${scTheme.color.global.inputBackground};
  }

  &.ace-queryinput {
    background-color: ${scTheme.color.global.inputBackground};
    color: ${util.contrastingColor(scTheme.color.global.inputBackground, 'AAA')};
  }

  &.ace-queryinput .ace_cursor {
    color: ${scTheme.color.gray[50]};
  }

  &.ace-queryinput .ace_marker-layer .ace_selection {
    background: ${scTheme.color.variant.lightest.default};
  }

  &.ace-queryinput.ace_multiselect .ace_selection.ace_start {
    box-shadow: 0 0 3px 0 ${scTheme.color.global.inputBackground};
  }

  &.ace-queryinput .ace_marker-layer .ace_step {
    background: ${scTheme.color.variant.warning};
  }

  &.ace-queryinput .ace_marker-layer .ace_bracket {
    margin: -1px 0 0 -1px;
    border: none;
  }

  &.ace-queryinput .ace_marker-layer .ace_active-line {
    background: ${scTheme.color.global.inputBackground};
  }

  &.ace-queryinput .ace_gutter-active-line {
    background-color: ${scTheme.color.global.inputBackground};
  }

  &.ace-queryinput .ace_marker-layer .ace_selected-word {
    border: 1px solid ${scTheme.color.gray[80]};
  }

  &.ace-queryinput .ace_invisible {
    color: ${scTheme.color.global.inputBackground};
  }

  &.ace-queryinput .ace_keyword,
  &.ace-queryinput .ace_meta,
  &.ace-queryinput .ace_storage,
  &.ace-queryinput .ace_storage.ace_type,
  &.ace-queryinput .ace_support.ace_type {
    color: ${scTheme.color.variant.primary};
  }

  &.ace-queryinput .ace_keyword.ace_operator {
    color: ${scTheme.color.variant.darker.info};
  }

  &.ace-queryinput .ace_constant.ace_character,
  &.ace-queryinput .ace_constant.ace_language,
  &.ace-queryinput .ace_constant.ace_numeric,
  &.ace-queryinput .ace_keyword.ace_other.ace_unit,
  &.ace-queryinput .ace_support.ace_constant,
  &.ace-queryinput .ace_variable.ace_parameter {
    color: ${scTheme.color.variant.dark.danger};
  }

  &.ace-queryinput .ace_constant.ace_other {
    color: ${scTheme.color.variant.default};
  }

  &.ace-queryinput .ace_invalid {
    color: ${util.readableColor(scTheme.color.brand.primary)};
    background-color: ${scTheme.color.brand.primary};
  }

  &.ace-queryinput .ace_invalid.ace_deprecated {
    color: ${util.readableColor(scTheme.color.brand.primary)};
    background-color: ${scTheme.color.variant.dark.primary};
  }

  &.ace-queryinput .ace_fold {
    background-color: ${scTheme.color.variant.info};
    border-color: ${util.contrastingColor(scTheme.color.global.inputBackground, 'AAA')};
  }

  &.ace-queryinput .ace_entity.ace_name.ace_function,
  &.ace-queryinput .ace_support.ace_function,
  &.ace-queryinput .ace_variable,
  &.ace-queryinput .ace_term {
    color: ${scTheme.color.variant.info};
  }

  &.ace-queryinput .ace_support.ace_class,
  &.ace-queryinput .ace_support.ace_type {
    color: ${scTheme.color.variant.dark.warning};
  }

  &.ace-queryinput .ace_heading,
  &.ace-queryinput .ace_markup.ace_heading,
  &.ace-queryinput .ace_string {
    color: ${scTheme.color.variant.dark.success};
  }

  &.ace-queryinput .ace_entity.ace_name.ace_tag,
  &.ace-queryinput .ace_entity.ace_other.ace_attribute-name,
  &.ace-queryinput .ace_meta.ace_tag,
  &.ace-queryinput .ace_string.ace_regexp,
  &.ace-queryinput .ace_variable {
    color: ${scTheme.color.brand.primary};
  }

  &.ace-queryinput .ace_comment {
    color: ${scTheme.color.gray[60]};
  }

  .ace_placeholder {
    font-size: 16px;
    padding: 0 !important;
    font-family: inherit !important;
  }

  &.ace-queryinput .ace_indent-guide {
    background: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAACCAYAAACZgbYnAAAAE0lEQVQImWP4////f4bdu3f/BwAlfgctduB85QAAAABJRU5ErkJggg==) right repeat-y;
  }

  .ace_editor.ace_autocomplete {
    width: 600px !important;
  }
`);

export default StyledAceEditor;
