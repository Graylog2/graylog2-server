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
import { createGlobalStyle, css } from 'styled-components';

const GlobalThemeStyles = createGlobalStyle(({ theme }) => css`
  #editor {
    height: 256px;
  }

  html {
    font-size: ${theme.fonts.size.root} !important; /* override Bootstrap default */
  }

  body {
    background-color: ${theme.colors.global.background};
    color: ${theme.colors.global.textDefault};
    font-family: ${theme.fonts.family.body};
    overflow-x: hidden;
    margin-top: 50px;
    min-height: calc(100vh - 50px);
  }

  ul {
    list-style-type: none;
    margin: 0;
  }

  ul.no-padding {
    padding: 0;
  }

  hr {
    border-top: 1px solid ${theme.colors.global.background};
  }

  h1,
  h2,
  h3,
  h4,
  h5,
  h6 {
    font-weight: normal;
    padding: 0;
    margin: 0;
    color: ${theme.colors.global.textDefault};
  }

  h1 {
    font-size: ${theme.fonts.size.h1};
  }

  h2 {
    font-size: ${theme.fonts.size.h2};
  }

  h3 {
    font-size: ${theme.fonts.size.h3};
  }

  h4 {
    font-size: ${theme.fonts.size.h4};
  }

  h5 {
    font-size: ${theme.fonts.size.h5};
  }

  h6 {
    font-size: ${theme.fonts.size.h6};
    font-weight: bold;
  }

  a {
    color: ${theme.colors.global.link};
  }

  a:hover {
    color: ${theme.colors.global.linkHover};
  }

  /* Remove boostrap outline */
  a:active,
  select:active,
  input[type="file"]:active,
  input[type="radio"]:active,
  input[type="checkbox"]:active,
  .btn:active {
    outline: none;
    outline-offset: 0;
  }

  input.form-control,
  select.form-control,
  textarea,
  textarea.form-control {
    color: ${theme.colors.input.color};
    background-color: ${theme.colors.input.background};
    border-color: ${theme.colors.input.border};
    font-family: ${theme.fonts.family.body};

    &::placeholder {
      color: ${theme.colors.input.placeholder};
    }

    &:focus {
      border-color: ${theme.colors.input.borderFocus};
      box-shadow: ${theme.colors.input.boxShadow};
    }
    
    &[disabled],
    &[readonly],
    fieldset[disabled] & {
      background-color: ${theme.colors.input.backgroundDisabled};
      color: ${theme.colors.input.colorDisabled};
    }
  }

  legend small {
    color: ${theme.colors.gray[60]};
    margin-left: 5px;
  }

  small {
    font-size: ${theme.fonts.size.small};
  }

  .input-group-addon.input-group-separator {
    border-right-width: 0;
    border-left-width: 0;
  }

  .content {
    padding-top: 15px;
    padding-bottom: 15px;
    background-color: ${theme.colors.global.contentBackground};
    border: 1px solid ${theme.colors.gray[80]};
    margin-bottom: 10px;

    p.description {
      margin-top: 3px;
      color: ${theme.colors.gray[50]};
    }
  }

  .actions-lg .actions-container {
    margin-top: 10px;
    padding-left: 50px;
  }

  .content p.description-tooltips {
    margin-top: 10px;
  }

  .notifications-none {
    margin-top: 10px;
  }

  .row {
    margin-bottom: 15px;
  }

  .no-bm {
    margin-bottom: 0;
  }

  .has-bm {
    margin-bottom: 10px;
  }

  .alert {
    margin-bottom: 0;
    margin-top: 5px;
  }

  .modal form {
    margin-bottom: 0;
  }

  .alert-bar {
    margin: 10px -20px 0;
  }

  .xtrc-converter-subfields {
    margin-left: 20px;
  }

  .u-light {
    border-bottom: 1px dotted ${theme.colors.gray[70]};
    margin-bottom: 5px;
    padding-bottom: 5px;
  }

  .input-docs {
    margin-left: 3px;
  }

  .input-docs:hover {
    text-decoration: none;
  }

  .timerange-selector select {
    margin-bottom: 0;
  }

  .master-node {
    color: ${theme.colors.variant.dark.warning};
  }

  .loglevel-metrics-row {
    margin-top: 2px;
    margin-left: 10px;
  }

  .loglevel-metrics dl {
    margin-bottom: 5px;
    margin-top: 5px;
  }

  .loglevel-metrics dt {
    float: left;
    margin-right: 5px;
  }

  .subsystems {
    margin-top: 10px;
    margin-left: 10px;
  }

  .sources.overlay {
    background-color: ${theme.colors.gray[60]};
    height: 200px;
    line-height: 200px;
    opacity: 0.2;
    position: absolute;
    text-align: center;
    font-size: 50px;
  }

  .metrics-filter {
    margin-bottom: 15px !important;
  }

  dl.metric-def dt {
    float: left;
  }

  dl.metric-timer dd {
    margin-left: 125px;
  }

  dl.metric-meter dd {
    margin-left: 95px;
  }

  dl.metric-gauge dd {
    margin-left: 80px;
  }

  dl.metric-counter dd {
    margin-left: 80px;
  }

  dl.metric-histogram dd {
    margin-left: 125px;
  }

  td.centered {
    text-align: center;
  }

  td.limited {
    max-width: 180px;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .configuration-field-optional {
    margin-left: 5px;
    font-size: ${theme.fonts.size.small};
  }

  .index-description {
    margin-bottom: 7px;
  }

  .index-description .index-info {
    margin-top: 5px;
  }

  .index-description .index-info dl {
    margin-top: 5px;
  }

  .index-details {
    margin-left: 5px;
  }

  .index-label {
    vertical-align: text-top;
  }

  .index-label .label {
    margin-right: 5px;
  }

  .index-more-actions {
    font-size: 90%;
    margin-left: 5px;
  }

  .shard-meters {
    margin-top: 10px;
  }

  .shard-meters dl {
    margin-bottom: 0;
    margin-top: 0;
  }

  .shard-meters dl dt {
    float: left;
  }

  .shard-meters dl dd {
    margin-left: 65px;
  }

  .shards {
    margin: 0;
    padding: 0;
    margin-top: 5px;
  }

  .system-system dt {
    float: left;
  }

  .system-system dd {
    margin-left: 75px;
  }

  dl.system-journal {
    margin-top: 5px;
    margin-bottom: 0;
  }

  .system-journal dt {
    float: left;
  }

  .system-journal dd {
    margin-left: 120px;
  }

  dl.system-dl {
    margin: 0;
  }

  .system-dl dt {
    float: left;
    clear: left;
  }

  .system-dl dd {
    margin-left: 180px;
  }

  .system-rest dt {
    float: left;
  }

  .system-rest dd {
    margin-left: 120px;
  }

  .search-help {
    margin: 0 5px;
    line-height: 34px;
  }

  .no-widgets {
    margin-top: 15px;
  }

  table .dc-table-column {
    word-break: break-all;
  }

  .sources th {
    background-color: ${theme.colors.gray[20]};
    color: ${theme.utils.readableColor(theme.colors.gray[20])};
    font-weight: normal;
  }

  .sources .dc-table-column._3 {
    padding-right: 0;
    text-align: right;
  }

  .parse-error {
    background-color: ${theme.colors.variant.light.danger};
    color: ${theme.utils.contrastingColor(theme.colors.variant.light.danger)};
    padding-left: 2px;
    padding-right: 2px;
  }

  .add-alert-type {
    margin-bottom: 0;
  }

  .alerts {
    margin-top: 15px;
  }

  .alerts tbody {
    border: none;
  }

  .alerts th {
    background-color: ${theme.colors.gray[10]};
    color: ${theme.colors.global.textAlt};
    font-weight: normal;
  }

  .alerts th a {
    display: block;
  }

  .alerts th a:focus {
    color: ${theme.colors.global.textAlt};
  }

  .result-highlight-colored {
    background-color: ${theme.colors.variant.warning};
  }

  .annotation .content {
    margin-bottom: 10px;
    cursor: auto !important;
  }

  .scrollable-table {
    width: 100%;
    overflow: auto;
  }

  .well.configuration-well {
    margin-top: 5px;
    margin-bottom: 0;
    padding: 9px;
    font-family: ${theme.fonts.family.monospace};
    word-wrap: break-word;
  }

  .well.configuration-well > ul {
    padding: 0;
    margin: 0;
  }

  .well.react-configuration-well {
    white-space: pre-line;
  }

  .well.configuration-well .configuration-section {
    margin-bottom: 10px;
  }

  .well.configuration-well li:not(:last-child) {
    margin-bottom: 5px;
  }

  .well.configuration-well .key {
    display: inline;
  }

  .alert-callback .well.configuration-well .key {
    display: inline-block;
    min-width: 140px;
    vertical-align: top;
  }

  .well.configuration-well .value {
    display: inline;
  }

  .alert-callback .well.configuration-well .value {
    display: inline-block;
  }

  .form-inline label {
    margin-right: 10px;
  }

  .form-horizontal .help-block.help-standalone {
    line-height: 20px;
    margin-top: 7px;
  }

  .form-horizontal.pull-left label.control-label {
    width: auto;
  }

  .form-horizontal.pull-left div.controls {
    display: inline-block;
    float: right;
    margin-left: 20px;
  }

  form.extractor-form .control-group label {
    display: inline-block;
  }

  .configuration-bundles input[type="file"] {
    line-height: inherit !important;
  }

  table th.actions,
  table td.actions {
    width: 110px;
  }

  .btn-text {
    font-family: ${theme.fonts.family.body};
    font-size: ${theme.fonts.size.small};
    padding: 0;
    vertical-align: baseline;
  }

  .message-loader-form input {
    margin-right: 5px;
  }

  nav.navbar-fixed-top ul.dropdown-menu li a {
    font-size: ${theme.fonts.size.body};
  }

  nav.navbar-fixed-top ul.dropdown-menu {
    padding-top: 10px;
    padding-bottom: 10px;
  }

  nav.navbar-fixed-top ul.dropdown-menu li {
    padding: 2px 2px 2px 0;
  }

  nav.navbar-fixed-top ul.dropdown-menu li.divider {
    padding: 0;
  }

  table.indexer-failures {
    margin-top: 10px;
  }

  div.row-sm {
    margin-bottom: 5px;
  }

  dl.system-system,
  dl.system-rest {
    margin-top: 5px;
    margin-bottom: 0;
  }

  .table-sm {
    margin-bottom: 0;
  }

  .graylog-input-metrics {
    margin-top: 5px;
  }

  .filter .form-inline .form-group {
    display: inline-block;
    margin-bottom: 0;
    vertical-align: middle;
  }

  div.alert-callback hr {
    margin-top: 10px;
    margin-bottom: 10px;
  }

  div.alert-callbacks {
    margin-top: 10px;
  }

  .alarm-callbacks {
    padding: 0;
  }

  .alarm-callbacks li:not(:last-child) {
    margin-bottom: 10px;
    padding-bottom: 10px;
    border-bottom: 1px solid ${theme.colors.gray[90]};
  }

  .threaddump {
    font-size: ${theme.fonts.size.small};
  }

  h2.extractor-title {
    margin-bottom: 2px;
  }

  .stream-loader {
    margin-top: 5px;
  }

  .form-inline .typeahead-wrapper {
    display: inline-block;
    vertical-align: middle;
    width: auto;
  }
  
  .typeahead-wrapper .tt-menu {
    background-color: ${theme.colors.global.contentBackground};
    box-shadow: 0 3px 3px ${theme.colors.global.navigationBoxShadow};
    color: ${theme.colors.global.textDefault};
    
    .tt-suggestion:hover,
    .tt-suggestion.tt-cursor {
      color: ${theme.colors.variant.darkest.info};
      background-color: ${theme.colors.variant.lighter.info};
      background-image: none;
    }
  }

  .form-group-inline {
    display: inline-block;
    margin: 0;
  }

  ul.tag-list,
  ul.pill-list {
    display: inline-block;
    list-style: none;
    padding: 0;
    position: relative;
  }

  ul.pill-list {
    margin-left: 10px;
    vertical-align: middle;
  }

  ul.tag-list > li,
  ul.pill-list > li {
    display: inline-block;
    vertical-align: middle;
  }

  .tags-input ul.tag-list > li {
    padding-top: 10px;
    padding-bottom: 5px;
  }

  .tag,
  .pill {
    font-size: ${theme.fonts.size.body};
    margin-right: 5px;
  }

  .pill {
    color: ${theme.colors.global.textDefault};
    background-color: ${theme.colors.gray[90]};
    padding: 6px 12px;
  }

  .tag-remove,
  .pill-remove {
    color: ${theme.colors.global.textDefault};
    cursor: pointer;
    margin-left: 5px;
  }

  .tag-remove::before,
  .pill-remove::before {
    content: "×";
  }

  .save-button-margin {
    margin-right: 5px;
  }

  .form-control.message-id-input {
    width: 300px;
  }

  /* additional styles for 'StyledAceEditor' */
  .ace_editor.ace_autocomplete.ace-queryinput {
    width: 600px !important;
    margin-top: 6px;
  }

  code {
    color: ${theme.colors.variant.darker.danger};
    background-color: ${theme.colors.variant.lightest.danger};
  }

  pre {
    color: ${theme.colors.variant.darker.default};
    background-color: ${theme.colors.variant.lightest.default};
    border-color: ${theme.colors.variant.lighter.default};
  }
  
  input[type="range"],
  input[type="range"]:focus {
    box-shadow: none;
    height: auto;
  }
`);

export default GlobalThemeStyles;
