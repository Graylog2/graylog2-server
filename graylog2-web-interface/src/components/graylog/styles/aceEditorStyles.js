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
import { css } from 'styled-components';

const aceEditorStyles = ({ colors }) => {
  return css`
    .ace_editor {
      border: 1px solid ${colors.gray[80]};
      border-radius: 5px;
    }

    .ace-graylog {
      background-color: ${colors.global.contentBackground};
      color: ${colors.variant.darkest.default};

      &.ace_multiselect .ace_selection.ace_start {
        box-shadow: 0 0 3px 0 ${colors.global.contentBackground};
      }

      .ace_gutter {
        background: ${colors.variant.lightest.default};
        color: ${colors.variant.darkest.default};
      }

      .ace_print-margin {
        width: 1px;
        background: ${colors.variant.lightest.default};
      }

      .ace_cursor {
        color: ${colors.variant.light.default};
      }

      .ace_marker-layer {
        .ace_selection {
          background: ${colors.gray[70]};
        }

        .ace_step {
          background: rgb(255, 255, 0);
        }

        .ace_bracket {
          margin: -1px 0 0 -1px;
          border: 1px solid ${colors.gray[70]};
        }

        .ace_selected-word {
          border: 1px solid ${colors.gray[60]};
        }

        .ace_active-line {
          background: ${colors.gray[90]};
        }
      }

      .ace_gutter-active-line {
        background-color: ${colors.gray[80]};
      }

      .ace_invisible {
        color: ${colors.gray[70]};
      }

      .ace_keyword,
      .ace_meta,
      .ace_storage,
      .ace_storage.ace_type,
      .ace_support.ace_type {
        color: ${colors.global.link};
      }

      .ace_keyword.ace_operator {
        color: ${colors.variant.darker.info};
      }

      .ace_constant.ace_character,
      .ace_constant.ace_language,
      .ace_constant.ace_numeric,
      .ace_keyword.ace_other.ace_unit,
      .ace_support.ace_constant,
      .ace_variable.ace_parameter {
        color: ${colors.variant.darker.primary};
      }

      .ace_constant.ace_other {
        color: ${colors.variant.darker.default};
      }

      .ace_invalid {
        color: ${colors.global.textAlt};
        background-color: ${colors.variant.light.danger};
      }

      .ace_invalid.ace_deprecated {
        color: ${colors.global.textAlt};
        background-color: ${colors.variant.primary};
      }

      .ace_fold {
        background-color: ${colors.variant.info};
        border-color: ${colors.variant.darkest.default};
      }

      .ace_entity.ace_name.ace_function,
      .ace_support.ace_function,
      .ace_variable {
        color: ${colors.variant.info};
      }

      .ace_support.ace_class,
      .ace_support.ace_type {
        color: ${colors.variant.darker.warning};
      }

      .ace_heading,
      .ace_markup.ace_heading,
      .ace_string {
        color: ${colors.variant.darker.success};
      }

      .ace_entity.ace_name.ace_tag,
      .ace_entity.ace_other.ace_attribute-name,
      .ace_meta.ace_tag,
      .ace_string.ace_regexp,
      .ace_variable {
        color: ${colors.variant.light.danger};
      }

      .ace_comment {
        color: ${colors.variant.dark.default};
      }
    }
  `;
};

export default aceEditorStyles;
