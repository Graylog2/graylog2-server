import { createGlobalStyle, css } from 'styled-components';

import openSansRegular from './fonts/OpenSans-Regular.woff';
import openSansRegular2 from './fonts/OpenSans-Regular.woff2';
import openSansItalic from './fonts/OpenSans-Italic.woff';
import openSansItalic2 from './fonts/OpenSans-Italic.woff2';
import openSansBold from './fonts/OpenSans-Bold.woff';
import openSansBold2 from './fonts/OpenSans-Bold.woff2';

const fontFamily = '"Open Sans", "Helvetica Neue", Helvetica, Arial, sans-serif';

const GlobalThemeStyles = createGlobalStyle(({ theme }) => css`
  @font-face {
    font-family: 'Open Sans';
    font-style: normal;
    font-weight: 400;
    src: local('Open Sans'),
      local('OpenSans'),
      url(${openSansRegular2}) format('woff2'),
      url(${openSansRegular}) format('woff');
  }

  @font-face {
    font-family: 'Open Sans';
    font-style: normal;
    font-weight: 700;
    src: local('Open Sans'),
      local('OpenSans'),
      url(${openSansBold2}) format('woff2'),
      url(${openSansBold}) format('woff');
  }

  @font-face {
    font-family: 'Open Sans';
    font-style: italic;
    font-weight: 400;
    src: local('Open Sans'),
      local('OpenSans'),
      url(${openSansItalic2}) format('woff2'),
      url(${openSansItalic}) format('woff');
  }

  #editor {
    height: 256px;
  }

  body {
    background-color: ${theme.color.global.background};
    font-family: ${fontFamily};
    font-size: 12px;
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
    border-top: 1px solid ${theme.color.global.background};
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
    color: ${theme.color.global.textDefault};
  }

  h1 {
    font-size: 28px;
  }

  h2 {
    font-size: 21px;
  }

  h3 {
    font-size: 18px;
  }

  h4 {
    font-size: 14px;
    font-weight: normal;
  }

  a {
    color: ${theme.color.global.link};
  }

  a:hover {
    color: ${theme.color.global.linkHover};
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
  textarea.form-control {
    color: ${theme.color.gray[30]};
    font-family: ${fontFamily};

    &:hover {
      border-color: hsl(0, 0%, 70%);
    }
  }

  label {
    font-size: 14px;
  }

  legend small {
    color: ${theme.color.gray[60]};
    margin-left: 5px;
  }

  .input-group-addon.input-group-separator {
    border-right-width: 0;
    border-left-width: 0;
  }

  .content {
    padding-top: 15px;
    padding-bottom: 15px;
    background-color: ${theme.color.global.contentBackground};
    border: 1px solid ${theme.color.gray[80]};
    margin-bottom: 10px;
  }

  .content p.description {
    margin-top: 3px;
    color: ${theme.color.gray[50]};
  }

  .actions-lg .actions-container {
    height: 60px;
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
    border-bottom: 1px dotted ${theme.color.gray[70]};
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
    color: #f89406;
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
    background-color: ${theme.color.gray[60]};
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

  #user-list th.user-type {
    width: 50px;
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
    font-size: 11px;
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
    background-color: #333;
    color: ${theme.color.global.textAlt};
    font-weight: normal;
  }

  .sources .dc-table-column._3 {
    padding-right: 0;
    text-align: right;
  }

  .parse-error {
    background-color: #f2dede;
    color: #a94442;
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
    background-color: ${theme.color.gray[10]};
    color: ${theme.color.global.textAlt};
    font-weight: normal;
  }

  .alerts th a {
    display: block;
  }

  .alerts th a:focus {
    color: ${theme.color.global.textAlt};
  }

  .result-highlight-colored {
    background-color: ${theme.color.variant.warning};
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
    font-family: monospace;
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
    font-family: ${fontFamily};
    font-size: 12px;
    padding: 0;
    vertical-align: baseline;
  }

  .message-loader-form input {
    margin-right: 5px;
  }

  nav.navbar-fixed-top ul.dropdown-menu li a {
    font-size: 12px;
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
    border-bottom: 1px solid ${theme.color.gray[90]};
  }

  .threaddump {
    font-size: 11px;
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
    font-size: 14px;
    margin-right: 5px;
  }

  .pill {
    color: ${theme.color.global.textDefault};
    background-color: ${theme.color.gray[90]};
    padding: 6px 12px;
  }

  .tag-remove,
  .pill-remove {
    color: ${theme.color.global.textDefault};
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
`);

export default GlobalThemeStyles;
