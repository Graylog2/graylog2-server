import { createGlobalStyle, css } from 'styled-components';

const GlobalThemeStyles = createGlobalStyle(({ theme }) => css`
  #editor {
    height: 256px;
  }

  body {
    background-color: ${theme.color.global.background};
    font-family: "Open Sans", "Helvetica Neue", Helvetica, Arial, sans-serif;
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
    font-family: "Open Sans", "Helvetica Neue", Helvetica, Arial, sans-serif;

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

  .content p.description-tooltips-small .fa-stack {
    margin-right: 1px;
    position: relative;
    top: -1px;
  }

  .content p.description-tooltips {
    margin-top: 10px;
  }

  .content-col {
    padding: 15px 10px;
    background-color: ${theme.color.global.contentBackground};
    border: 1px solid ${theme.color.gray[80]};
    margin-top: 15px;
  }

  #main-row {
    margin-bottom: 0;
  }

  #main-content {
    margin-top: 10px;
    padding: 5px 25px;
  }

  .support-sources ul {
    margin: 0;
    padding: 0;
    margin-top: 5px;
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

  .selected-resolution {
    font-weight: bold;
  }

  .modal form {
    margin-bottom: 0;
  }

  .input-new {
    margin-bottom: 8px;
  }

  .alert-bar {
    margin: 10px -20px 0;
  }

  .xtrc-new-example {
    margin-bottom: 5px;
    font-family: monospace;
    font-size: 14px;
    white-space: pre-wrap;
    word-wrap: break-word;
  }

  .xtrc-no-example {
    margin-top: 15px;
    margin-bottom: 12px;
  }

  #create-extractor {
    margin-top: 10px;
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

  .timerange-chooser {
    float: left;
    margin-right: 5px;
  }

  .timerange-chooser .btn {
    padding: 6px 7px;
    line-height: 15px;
    font-size: 12px;
  }

  .timerange-chooser .btn .caret {
    margin-left: 1px;
  }

  .timerange-chooser .selected a {
    font-weight: bold;
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

  .alpha80 {
    background: rgb(0, 0, 0) transparent;
    background: rgba(0, 0, 0, 0.8);
    filter: ~"progid:DXImageTransform.Microsoft.gradient(startColorstr=#99000000, endColorstr=#99000000)";
  }

  #scroll-to-hint {
    position: fixed;
    left: 50%;
    margin-left: -125px;
    top: 50px;
    color: ${theme.color.global.textAlt};
    font-size: 80px;
    padding: 25px;
    z-index: 2000;
    width: 200px;
    text-align: center;
    cursor: pointer;
    border-radius: 10px;
  }

  .graph-range-selector {
    outline: 1px solid ${theme.color.gray[90]};
    background: rgba(0, 0, 0, 0.3);
    position: absolute;
    top: 0;
    z-index: 1;
    cursor: pointer;
    pointer-events: none;
  }

  .detail_swatch {
    display: inline-block;
    width: 10px;
    height: 10px;
    margin: 0 4px 0 0;
  }

  #field-graphs .spinner {
    margin-bottom: 10px;
    text-align: center;
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

  ul.index-list {
    margin-left: 25px;
    margin-top: 5px;
    list-style-type: square;
  }

  .metric-list {
    padding: 0;
  }

  .metric-list li {
    margin-bottom: 5px;
  }

  .metric-list li .prefix {
    color: ${theme.color.gray[60]};
  }

  .metric-list li .name {
    font-size: 13px;
    font-family: monospace;
    word-break: break-all;
  }

  .metric-list li .metric {
    margin-left: 10px;
    padding: 10px;
  }

  .metric-list li .metric h3 {
    margin-bottom: 5px;
  }

  .metric-list dl {
    margin-top: 0;
    margin-bottom: 0;
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

  .metric-list li .name .open:hover {
    text-decoration: none;
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

  .dashboard {
    color: ${theme.color.global.textDefault};
    margin: 0;
    width: 100%;
  }

  .dashboard .widget {
    height: inherit;
    margin: 0;
    padding: 20px;
    display: grid;
    display: -ms-grid;
    grid-template-rows: auto minmax(10px, 1fr);
    -ms-grid-rows: auto minmax(10px, 1fr);
    -ms-grid-columns: 1fr;
  }

  .dashboard .widget .widget-top {
    position: relative;
    margin-bottom: -15px;
    top: -5px;
    font-size: 11px;
    line-height: 11px;
  }

  .dashboard .widget .controls {
    display: none;
    position: relative;
    left: -3px;
  }

  .dashboard .widget .reloading {
    margin-right: 2px;
    font-weight: bold;
    color: ${theme.color.variant.dark.info};
    display: none;
  }

  .dashboard .widget .loading-failed {
    color: ${theme.color.variant.danger} !important;
  }

  .tooltip .tooltip-inner {
    max-width: 300px;
  }

  .tooltip .tooltip-inner .datapoint-info {
    text-align: left;
  }

  .tooltip .tooltip-inner .datapoint-info .date {
    color: ${theme.color.gray[90]};
  }

  .dashboard .widget .dc-chart {
    float: none;
  }

  .dashboard .widget .widget-title {
    font-size: 18px;
    height: 25px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .dashboard .widget .load-error {
    color: ${theme.color.variant.danger};
    margin-right: 5px;
  }

  .dashboard .widget .widget-update-info {
    text-align: left;
    float: left;
    font-size: 11px;
    position: absolute;
    bottom: 10px;
    width: 130px;
  }

  .dashboard .widget .configuration dt {
    text-transform: capitalize;
  }

  .dashboard .widget svg {
    overflow: hidden;
  }

  .dashboard .widget .quickvalues-graph {
    text-align: center;
  }

  .dashboard .widget .graph.scatterplot path.line {
    display: none;
  }

  .dashboard .widget .actions {
    position: absolute;
    right: 15px;
    bottom: 10px;
  }

  .dashboard .widget .actions div {
    display: inline-block;
    margin-left: 5px;
  }

  .dashboard .widget .actions button {
    padding: 0 5px 0 5px;
  }

  .dashboard .widget .not-available {
    font-size: 70px;
  }

  .dashboard .widget .loading,
  .dashboard .widget .not-available {
    line-height: 100px;
    text-align: center;
  }

  .dashboard .widget .loading .spinner,
  .dashboard .widget .not-available .spinner {
    vertical-align: middle;
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

  .card {
    padding: 12px 20px;
    font-size: 15px;
    font-weight: 300;
    background-color: ${theme.color.gray[100]};
    border: 1px solid ${theme.color.gray[90]};
    border-radius: 2px;
    margin: 10px;
  }

  .card h1 {
    margin-bottom: 5px;
    font-size: 28px;
    line-height: 1;
    letter-spacing: -1;
  }

  .card label {
    font-weight: inherit;
  }

  .card div {
    margin-left: 0;
  }

  .card ul li {
    margin-bottom: 5px;
  }

  .card.info p {
    margin: 0;
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

  .fa-mini {
    font-size: 10px;
  }

  .configuration-bundles input[type="file"] {
    line-height: inherit !important;
  }

  table th.actions,
  table td.actions {
    width: 110px;
  }

  .btn-text {
    font-family: 'Open Sans', sans-serif;
    font-size: 12px;
    padding: 0;
    vertical-align: baseline;
  }

  .message-loader-form input {
    margin-right: 5px;
  }

  ul.entity-list {
    padding: 0;
    margin: 0;

    li.entity-list-item {
      display: block;
      padding: 15px 0;

      h2 .label {
        margin-left: 5px;
        line-height: 2;
        vertical-align: bottom;
      }

      .item-description {
        min-height: 17px;
        margin: 5px 0;
      }

      .item-actions > .btn,
      .item-actions > .btn-group,
      .item-actions > span > .btn {
        margin-left: 5px;
        margin-bottom: 5px;
      }
    }

    li.entity-list-item:not(:last-child) {
      border-bottom: 1px solid ${theme.color.variant.light.info};
    }
  }

  dl.message-details {
    margin-top: 10px;
    margin-bottom: 0;
  }

  dl.message-details dt {
    font-weight: bold;
    margin-left: 1px;
  }

  dl.message-details dd {
    margin-bottom: 5px;
    padding-bottom: 5px;
    margin-left: 1px; /* Ensures that italic text is not cut */
  }

  dl.message-details-fields span:not(:last-child) dd {
    border-bottom: 1px solid ${theme.color.gray[90]};
  }

  dl.message-details-fields dd {
    white-space: pre-wrap;
  }

  dl.message-details-fields .field-value {
    font-family: monospace;
  }

  dl.message-details-fields dd.message-field .field-value {
    max-height: 500px;
    overflow: auto;
  }

  dl.message-details dd.stream-list ul {
    list-style-type: disc;
    padding-left: 25px;
  }

  dl.message-details dd.stream-list ul li {
    margin-top: 3px;
  }

  dl.message-details dd div.message-field-actions {
    padding-left: 10px;
    position: relative;
    top: -10px;
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

  .react-input-metrics {
    margin-top: 5px;
    font-size: 13px;
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

  /* // Ensure that the stream start/pause buttons have the same size. */
  .toggle-stream-button {
    width: 8.5em;
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
