import { createGlobalStyle } from 'styled-components';

const GlobalThemeStyles = createGlobalStyle`
  #editor {
    height: 256px;
  }

  body {
    background-color: #e3e3e3;
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
    border-top: 1px solid #e3e3e3;
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
    color: #333;
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
    color: #16ace3;
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
    color: #666;
    font-family: "Open Sans", "Helvetica Neue", Helvetica, Arial, sans-serif;

    &:hover {
      border-color: hsl(0, 0%, 70%);
    }
  }

  label {
    font-size: 14px;
  }

  legend small {
    color: #aaa;
    margin-left: 5px;
  }

  .input-group-addon.input-group-separator {
    border-right-width: 0;
    border-left-width: 0;
  }

  .content {
    padding-top: 15px;
    padding-bottom: 15px;
    background-color: #fff;
    border: 1px solid #d1d1d1;
    margin-bottom: 10px;
  }

  .content p.description {
    margin-top: 3px;
    color: #939393;
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
    background-color: #fff;
    border: 1px solid #d1d1d1;
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
    border-bottom: 1px dotted #bbb;
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
    color: #fff;
    font-size: 80px;
    padding: 25px;
    z-index: 2000;
    width: 200px;
    text-align: center;
    cursor: pointer;
    border-radius: 10px;
  }

  .graph-range-selector {
    outline: 1px solid #eee;
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
    background-color: #aaa;
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
    color: #aaa;
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
    color: #333;
    margin: 0;
    width: 100%;
  }

  .dashboard .widget {
    height: inherit;
    margin: 0;
    padding: 20px;
    display: grid;
    grid-template-rows: max-content minmax(10px, 1fr);
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
    color: #0085a7;
    display: none;
  }

  .dashboard .widget .loading-failed {
    color: #ff4646 !important;
  }

  .tooltip .tooltip-inner {
    max-width: 300px;
  }

  .tooltip .tooltip-inner .datapoint-info {
    text-align: left;
  }

  .tooltip .tooltip-inner .datapoint-info .date {
    color: #e3e5e5;
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
    color: #ff3b00;
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

  .shard-routing .shards .shard {
    padding: 10px;
    margin: 5px;
    width: 50px;
    float: left;
    text-align: center;
  }

  .shard-routing .shards .shard-started {
    background-color: #dff0d8;
  }

  .shard-routing .shards .shard-relocating {
    background-color: #de9df4;
  }

  .shard-routing .shards .shard-initializing {
    background-color: #f4ddbc;
  }

  .shard-routing .shards .shard-unassigned {
    background-color: #c3c3c3;
  }

  .shard-routing .shards .shard-primary .id {
    font-weight: bold;
    margin-bottom: 3px;
    border-bottom: 1px solid #000;
  }

  .shard-routing .description {
    font-size: 11px;
    margin-top: 2px;
    margin-left: 6px;
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
    color: #fff;
    font-weight: normal;
  }

  .sources .dc-table-column._3 {
    padding-right: 0;
    text-align: right;
  }

  .dynatable-per-page-label {
    font-size: 14px;
  }

  .dynatable-search {
    font-size: 14px;
  }

  .dynatable-search input,
  .dynatable-per-page select {
    position: relative;
    top: 3px;
    margin-left: 3px;
  }

  .dynatable-pagination-links li:first-child {
    display: none;
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

  .alert-type-form .help-text {
    color: #939393;
    font-size: 12px;
    margin-left: 10px;
  }

  .alert-type-form input[type=number],
  .alert-type-form input[type=text],
  .alert-type-form select {
    padding: 0 0 0 5px;
    height: 25px;
    vertical-align: baseline;
    width: 50px;
  }

  .alert-type-form input[type=text] {
    width: 200px;
  }

  .alert-type-form input.alert-type-title {
    width: 300px;
  }

  .add-alert-destination-type {
    margin-bottom: 0;
  }

  .alert-destination-form {
    display: none;
    font-size: 14px;
    margin-top: 15px;
  }

  .alert-receivers {
    padding-left: 10px;
  }

  .alerts {
    margin-top: 15px;
  }

  .alerts tbody {
    border: none;
  }

  .alerts th {
    background-color: #333;
    color: #fff;
    font-weight: normal;
  }

  .alerts th a {
    display: block;
  }

  .alerts th a:focus {
    color: #fff;
  }

  .alert-condition .in-grace {
    color: #8c8e86;
  }

  .query-exception {
    margin-bottom: 0;
  }

  input.required-input-highlight {
    border-color: rgb(233, 50, 45);
    color: rgb(233, 50, 45);
  }

  .widget .replay-link {
    color: #000;
  }

  .widget .replay-link:hover {
    text-decoration: none;
  }

  .zeroclipboard-is-hover {
    /* // via .btn-default:hover from bootstrap */
    color: #333;
    background-color: #d7d9d9;
    border-color: #c3c8c8; /* // bootstrap copy end */
    cursor: move;
  }

  .zeroclipboard-is-active {
    color: #1a5273;
  }

  .result-highlight-colored {
    background-color: #ffec3d;
  }

  .result-highlight-control label {
    display: inline-block;
    font-size: 1em;
    line-height: 20px;
  }

  .node-state {
    cursor: help;
  }

  .annotation .content {
    margin-bottom: 10px;
    cursor: auto !important;
  }

  textarea.textarea-xlarge {
    width: 95%;
    height: 300px;
  }

  .extractor-json {
    font-family: monospace;
    font-size: 13px;
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
    background-color: #fafafa;
    border: 1px solid #ececec;
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

  .card .configuration-bundles .bundle-preview {
    border: 1px solid #ececec;
    border-radius: 2px;
    background-color: #fff;
    padding: 20px;
  }

  .card .configuration-bundles .bundle-preview pre {
    background-color: #f5f5f5;
  }

  .configuration-bundles .accordion {
    background-color: #fff;
    border: 1px solid #ececec;
    border-radius: 2px;
  }

  .configuration-bundles .accordion-group {
    margin: 0;
    border: 0;
    border-bottom: 1px solid #ececec;
    border-radius: 0;
  }

  .configuration-bundles .accordion-inner {
    padding-left: 30px;
  }

  .configuration-bundles .accordion-inner .upload input[type="file"] {
    height: 32px;
  }

  .configuration-bundles .bundle-preview {
    background-color: #f5f5f5;
    border: 1px solid #e3e3e3;
    border-radius: 3px;
    box-shadow: 0 1px 1px rgba(0, 0, 0, 0.05) inset;
    padding: 10px;
  }

  .configuration-bundles .bundle-preview .preview-actions form {
    display: inline-block;
    margin-left: 20px;
    margin-bottom: 0;
  }

  .configuration-bundles .bundle-preview .preview-actions form:first-child {
    margin-left: 0;
  }

  .configuration-bundles .bundle-preview pre {
    background-color: #cfcfcf;
  }

  .configuration-bundles .bundle-preview dd {
    margin-bottom: 10px;
  }

  .bundle-preview ul,
  .bundle-preview ol {
    margin: 0 0 10px 25px;
  }

  .bundle-preview ul {
    list-style-type: circle;
  }

  .build-content-pack button.select-all {
    margin-top: 7px;
    padding: 0;
  }

  #react-configuration-bundles {
    font-size: 14px;
    font-weight: normal;
    line-height: 20px;
    margin-top: 15px;
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

  .sources-title {
    height: 22px;
    line-height: 22px;
    vertical-align: top;
  }

  .sources-filtering {
    margin-top: 10px;
  }

  .sources-filtering .control-group {
    margin-bottom: 0;
  }

  #dc-sources-pie-chart svg {
    margin-top: 20px;
  }

  #dc-sources-pie-chart g.pie-slice.highlighted {
    fill-opacity: 0.8;
  }

  .form-horizontal .control-group .controls .checkbox-control:first-child {
    padding-top: 5px;
  }

  .form-horizontal .control-group .controls .checkbox-control label.checkbox {
    display: inline-block;
  }

  .form-horizontal .control-group .controls .checkbox-control span.help-inline {
    vertical-align: top;
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

  #grok-pattern-list th.name {
    min-width: 200px;
  }

  #grok-pattern-list td {
    word-break: break-all;
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

  ul.streams {
    padding: 0;
    margin: 0;
  }

  ul.streams li.stream {
    display: block;
    padding: 15px 0;
  }

  ul.streams li.stream:not(:last-child) {
    border-bottom: 1px solid #6dcff6;
  }

  ul.streams li.stream .stream-data {
    margin-top: 8px;
  }

  ul.streams li.stream .stream-data .stream-actions {
    position: relative;
    float: right;
    right: 0;
    bottom: 20px;
  }

  ul.streams li.stream .stream-data .stream-actions form.action-form {
    display: inline-block;
  }

  ul.streams li.stream .stream-data .stream-actions .btn-delete {
    margin-left: 15px;
    margin-right: 15px;
  }

  ul.streams li.stream .stream-data .stream-actions .btn-delete.last {
    margin-right: 0;
  }

  ul.streams li.stream .stream-description {
    margin-bottom: 3px;
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
      border-bottom: 1px solid #6dcff6;
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
    border-bottom: 1px solid #ececec;
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

  .indexer-failures-body select.dynatable-per-page-select {
    position: relative;
    top: 0;
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

  .graylog-input {
    margin-top: 15px;
    border-bottom: 1px;
  }

  .graylog-input-metrics {
    margin-top: 5px;
  }

  .react-input-metrics {
    margin-top: 5px;
    font-size: 13px;
  }

  .alert-type-form label.radio-inline {
    margin-right: 0;
    vertical-align: baseline;
  }

  .alert-type-form .radio-inline input[type=radio] {
    position: relative;
    margin-left: -20px;
    margin-right: 5px;
  }

  .alert-type-form div.well {
    margin-bottom: 0;
  }

  .alert-type-form .threshold-type {
    margin: 0 5px;
    vertical-align: baseline;
  }

  .form-inline .alert-type-form .form-control {
    vertical-align: baseline;
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
    border-bottom: 1px solid #ececec;
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

  p.failure-exception {
    margin-top: 5px;
    margin-bottom: 10px;
    color: #aaa;
  }

  i.error-icon {
    position: relative;
    top: -3px;
  }

  .stream-description .fa-cube {
    margin-right: 5px;
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
    color: #333;
    background-color: #E3E5E5;
    padding: 6px 12px;
  }

  .tag-remove,
  .pill-remove {
    color: #333;
    cursor: pointer;
    margin-left: 5px;
  }

  .tag-remove::before,
  .pill-remove::before {
    content: "×";
  }

  .field-analyzer {
    margin-left: 0 !important;
    margin-top: 10px !important;
  }

  .save-button-margin {
    margin-right: 5px;
  }

  .form-control.message-id-input {
    width: 300px;
  }

  .dropdown-header {
    text-transform: uppercase;
    padding: 0 15px !important;
    font-weight: bold;
  }
`;

export default GlobalThemeStyles;
