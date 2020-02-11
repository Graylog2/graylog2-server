import { css } from 'styled-components';

const globalStyles = css`
  #editor {
    height: 256px;
  }

  body {
    background-color: #e3e3e3;
    font-family: 'Open Sans', sans-serif;
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

  h1, h2, h3, h4, h5, h6 {
    font-weight: normal;
    padding: 0;
    margin: 0;
    color: #333
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
    font-weight: bold;
  }

  h4 {
    font-size: 14px;
    font-weight: normal;
  }

  a {
    color: #16ace3;
  }

  /* Remove boostrap outline */
  a:active, select:active, input[type="file"]:active, input[type="radio"]:active, input[type="checkbox"]:active, .btn:active {
    outline: none;
    outline-offset: 0;
  }

  input.form-control, select.form-control, textarea.form-control {
    color: #666;
    font-family: "Open Sans", "Helvetica Neue", Helvetica, Arial, sans-serif;

    &:hover {
      border-color: hsl(0,0%,70%)
    }
  }

  .input-group-addon.input-group-separator {
    border-right-width: 0;
    border-left-width: 0;
  }

  label {
    font-size: 14px;
  }

  legend small {
    color: #aaa;
    margin-left: 5px;
  }

  .btn:focus {
    background-position: 0;
  }

  #footer {
    text-align: center;
    font-size: 11px;
    color: #aaa;
    margin-bottom: 15px;
  }

  .modal-title {
    font-size: 21px;
  }

  .modal-dialog {
    margin-top: 55px;
  }

  .modal .modal-backdrop {
    z-index: 0;
  }

  .modal-backdrop {
    height: 100000%;  // yes, really. this fixes the backdrop being cut off when the page is scrolled.
    z-index: 1030;
  }

  .navbar-brand {
    padding: 12px 15px 0 15px;
  }

  #universalsearch {
    padding: 15px;
    background-color: #fff;
    border: 1px solid #D1D1D1;
  }

  #universalsearch-container {
    padding: 0 25px;
  }

  .content {
    padding-top: 15px;
    padding-bottom: 15px;
    background-color: #fff;
    border: 1px solid #D1D1D1;
    margin-bottom: 10px;
  }

  .content p.description {
    margin-top: 3px;
  }

  .content-head {
    padding-bottom: 0px;
  }

  .content p.description {
    color: #939393;
  }

  .content-head p {
    margin-top: 15px;
  }

  .content-head .actions-lg {
    float: right;
  }

  .actions-lg .actions-container {
    height: 60px;
    margin-top: 10px;
    padding-left: 50px;
  }

  .content-head .actions-sm {
    padding-bottom: 15px;
  }

  .content-head .description-tooltips .fa-stack {
    margin-right: 3px;
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
    border: 1px solid #D1D1D1;
    margin-top: 15px;
  }

  #sidebar {
    padding-left: 0;

    @media (max-height: 991px) {
      padding-right: 0;
    }

    @media (min-width: 991px) {
      padding-right: 15px;
    }
  }

  #sidebar .affix {
    @media (max-width: 991px) {
      position: relative;
    }

    @media (min-width: 991px) {
      top: 45px;
      z-index: 1000;
      position: fixed;
    }
  }

  #sidebar .actions {
    margin-top: 10px;
  }

  #sidebar .actions>div {
    margin: 5px 5px 0 0;
  }

  #sidebar hr {
    margin-top: 15px;
    margin-bottom: 10px;
  }

  #main-row {
    margin-bottom: 0px;
  }

  #main-content-sidebar {
    padding-left: 0;
    padding-right: 0;
  }

  #main-content {
    margin-top: 10px;
    padding: 5px 25px;
  }

  // This is rendered inside a #main-content, and we need to compensate the margin-top to make it look nicer
  #main-content-search {
    margin-bottom: 0;
    margin-top: -15px;
  }

  #universalsearch {
    padding: 5px;
    margin-bottom: 0px;
    margin-top: 15px;
  }

  .universalsearch-timerange-selector {
    font-size: 12px;
  }

  .universalsearch-timerange-selector label {
    font-size: 12px;
    margin-left: 5px !important;
  }

  .universalsearch-timerange-selector .radio input {
    margin-left: -17px;
    position: relative;
    top: -2px;
  }

  .universalsearch-form {
    margin-bottom: 0;
  }

  .universalsearch-form #search-container {
    margin-left: 5px;
  }

  .universalsearch-form button {
    font-size: 13px;
  }

  .universalsearch-form button.btn-sm {
    font-size: 12px;
  }

  .universalsearch-form div .query {
    margin-left: 45px;
    margin-right: 25px;
  }

  .universalsearch-form .query input {
    font-family: monospace;
    font-size: 13px;
    width: 100%;
    border: 0px;
    background-color: #fff;
    box-shadow: none;
    color: inherit;
    -moz-box-shadow: none;
    -webkit-box-shadow: none;
    -webkit-transition: none;
    -moz-transition: none;
    transition: none;
    outline: none;
    padding: 0;
    margin-left: 5px;
    margin-bottom: 5px;
  }

  .universalsearch-form .form-group {
    margin-bottom: 0;
  }

  a.fields-set-chooser {
    margin-right: 2px;
    margin-left: 2px;
  }

  #result-graph-container {
    margin-right: 12px;
    margin-bottom: 25px;
  }

  #result-graph {
    margin-left: 40px;
    margin-top: 5px;
  }

  #result-graph-timeline {
    margin-left: 40px;
  }

  #y_axis {
    float: left;
    height: 200px;
    width: 40px;
  }

  .search-result-fields {
    margin-top: 3px;
    padding-left: 0;
  }

  .search-result-fields li {
    margin-top: 1px;
    margin-bottom: 1px;
    padding: 2px 0;
  }

  .search-result-fields label {
    font-size: 12px;
    display: inline;
    position: relative;
    top: 3px;
    font-weight: normal !important;
  }

  .search-result-fields .form-group {
    margin-bottom: 0;
  }

  .search-result-fields .checkbox {
    margin: 0;
  }

  .search-result-fields input[type=checkbox] {
    position: relative;
    right: 4px;
  }

  .search-result-fields .dropdown .caret {
    display: none;
  }

  .search-result-fields button.dropdown-toggle {
    padding: 0;
  }

  .message-terms {
    margin-right: 8px;
    font-family: monospace;
  }

  .support-sources ul {
    margin-top: 5px;
  }

  .systemjob {
    margin-top: 10px;
    border-bottom: 1px solid #ccc;
  }

  .systemjob .progress {
    margin-top: 2px;
    margin-bottom: 5px;
  }

  .systemjob .finished {
    display: none;
    position: relative;
    top: -1px;
    left: 2px;
    font-family: "Arial", sans-serif;
    font-size: 11px;
  }

  .systemjobs-none {
    margin-top: 10px;
  }

  .notifications-none {
    margin-top: 10px;
  }

  .notification-badge-link:hover {
    border: 0px !important;
    text-decoration: none !important;
  }

  .row {
    margin-bottom: 15px;
  }

  .no-bm {
    margin-bottom: 0px;
  }

  .alert {
    margin-bottom: 0px;
    margin-top: 5px;
  }

  .system-messages {
    font-size: 12px;
  }

  .notification {
    margin-top: 10px !important;
  }

  .notification i {
    color: #000;
  }

  .notification-head {
    margin-bottom: 5px;
  }

  .notification-timestamp {
    margin-left: 3px;
    font-size: 10px;
  }

  .notification-solutions {
    margin-top: 10px;
  }

  .notification-solutions label {
    font-size: 13px;
  }

  .notification form {
    margin-bottom: 0px;
  }

  .notification-solutions h4 {
    font-size: 13px;
    font-weight: bold;
    margin-bottom: 1px;
  }

  .notification-solutions .btn {
    margin-top: 3px;
    font-size: 12px;
    padding: 3px 11px;
  }

  .node-row {
    border-bottom: 1px solid #ccc;
    padding-bottom: 8px;
    margin-top: 15px;
    margin-bottom: 0px;
  }

  .change-message-processing {
    position: relative;
    top: -1px;
  }

  .es-cluster-status {
    margin-top: 10px;
    margin-bottom: 5px;
  }

  .node-row .hostname {
    font-size: 12px;
  }

  .graph-resolution-selector {
    margin-top: 5px;
    margin-bottom: 20px;
  }

  .graph-resolution-selector li {
    padding-right: 0;
  }

  .graph-resolution-selector a {
    text-transform: capitalize;
  }

  .selected-resolution {
    font-weight: bold;
  }

  .search-result-fields .fa-cog {
    position: relative;
    top: 3px;
  }

  .modal form {
    margin-bottom: 0;
  }

  s
  .input-list h2 {
    margin-bottom: 5px;
  }

  .node-row {
    margin-bottom: 8px;
    margin-top: 0px;
  }

  .node-row .well {
    margin-bottom: 0px;
    margin-top: 3px;
    font-family: monospace;
    font-size: 11px;
  }

  .node-row-info {
    position: relative;
    top: 2px;
  }

  .node-row-info form {
    display: inline;
  }

  .node-row-info .text {
    position: relative;
    top: 3px;
  }

  .node-row-info-negative-correction {
    position: relative;
    top: -15px;
  }

  .input-list .alert {
    margin-top: 10px;
  }

  .input-new {
    margin-bottom: 8px;
  }

  .alert-bar {
    margin: 10px -20px 0;
  }

  .system-messages a {
    color: #000;
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

  .xtrc-hl {
    background-color: #f8b9b7;
    padding-top: 3px;
    padding-bottom: 3px;
  }

  #create-extractor {
    margin-top: 10px;
  }

  .xtrc-converter-subfields {
    margin-left: 20px;
  }

  .xtrc-list-container {
    margin-top: 15px;
  }

  .xtrc-list-container h2 {
    margin-bottom: 5px;
  }

  .xtrc-list-container .alert {
    margin-top: 5px;
  }

  .xtrc-list-drag {
    margin-top: 10px;
  }

  .manual-selector-form {
    margin-top: 5px;
  }

  .node-row .xtrc-converters {
    margin-top: 10px;
  }

  .node-row .xtrc-config li {
    margin-left: 10px;
  }

  .node-row .xtrc-converters li {
    margin-left: 10px;
  }

  .node-row .xtrc-converter-config li {
    margin-left: 20px;
  }

  .u-light {
    border-bottom: 1px dotted #bbb;
    margin-bottom: 5px;
    padding-bottom: 5px;
  }

  .xtrc-metrics {
    margin-top: 7px;
    margin-left: 5px;
  }

  .xtrc-metrics .span4 {
    margin-left: 0px;
  }

  .xtrc-metrics h3 .unit {
    font-size: 11px;
  }

  .xtrc-metrics dl {
    margin-top: 3px;
    margin-bottom: 0px;
  }

  .xtrc-metrics dt {
    float: left;
  }

  .xtrc-metrics dd {
    margin-left: 130px;
  }

  .xtrc-exception-bubble {
    position: relative;
    top: -7px;
    margin-left: 2px;
    cursor: pointer;
  }

  .extractor-condition-summary {
    margin-left: 5px;
    margin-top: 10px;
  }

  .success-match {
    color: #408140;
  }

  .fail-match {
    color: #da4f49;
  }

  .input-docs {
    margin-left: 3px;
  }

  .input-docs:hover {
    text-decoration: none;
  }

  .open-analyze-field {
    cursor: pointer;
    font-size: 16px;
    position: relative;
    top: 4px;
    color: #16ACE3;
  }

  .open-analyze-field-active {
    color: #1189B5;
  }

  .open-analyze-field:hover {
    color: #1189B5;
  }

  .analyze-field {
    margin-top: 10px;
  }

  .analyzer-content dl {
    margin-top: 10px;
    margin-bottom: 5px;
    border-left: 3px solid #eee;
    padding-left: 7px;
    padding-top: 1px;
    padding-bottom: 1px;
  }

  .analyzer-content dt {
    float: left;
    height: 25px;
  }

  .analyzer-content dd {
    margin-left: 120px;
    height: 25px;
  }

  .analyze-field .statistics .wrong-type {
    margin-top: 5px;
    color: #bbb;
  }

  .timerange-selector-container {
    border-bottom: 1px solid #eee;
    padding-bottom: 6px;
    margin-bottom: 8px;
    margin-left: 5px;
  }

  .timerange-selector-container .input-prepend, .input-append {
    margin-bottom: 0;
  }

  .timerange-selector select {
    margin-bottom: 0;
  }

  //#relative-timerange-selector {
  //  font-size: 12px;
  //  line-height: 15px;
  //  height: 25px;
  //}

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

  .permission-select {
    width: 350px;
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

  .subsystems-row {
    margin-bottom: 10px;
  }

  #universalsearch .timerange-selector-container .keyword .icon {
    margin-left: 5px;
  }

  #universalsearch .timerange-selector-container .keyword .keyword-preview {
    margin-top: 4px;
    margin-left: 45px;
    font-size: 11px;
  }

  #universalsearch .timerange-selector-container .keyword .keyword-preview .fromto span {
    background-color: #fec;
    padding-left: 3px;
    padding-right: 3px;
  }

  .no-active-nodes {
    margin-top: 8px;
  }

  .alpha80 {
    background: rgb(0, 0, 0) transparent;
    background: rgba(0, 0, 0, 0.8);
    filter: ~"progid:DXImageTransform.Microsoft.gradient(startColorstr=#99000000, endColorstr=#99000000)";
    -ms-filter: ~"progid:DXImageTransform.Microsoft.gradient(startColorstr=#99000000, endColorstr=#99000000)";
  }

  .alpha70 {
    background: rgb(0, 0, 0) transparent;
    background: rgba(0, 0, 0, 0.7);
    filter: ~"progid:DXImageTransform.Microsoft.gradient(startColorstr=#99000000, endColorstr=#99000000)";
    -ms-filter: ~"progid:DXImageTransform.Microsoft.gradient(startColorstr=#99000000, endColorstr=#99000000)";
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
    -webkit-border-radius: 10px 10px 10px 10px;
    -moz-border-radius: 10px 10px 10px 10px;
    cursor: pointer;
    border-radius: 10px 10px 10px 10px;
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
    margin: 0 4px 0 0
  }

  .input-io .total {
    color: #b8b8b8;
  }

  .input-list .static-fields {
    margin-top: 10px;
    margin-left: 3px;
  }

  .input-list .static-fields ul {
    margin: 0;
    padding: 0;
  }

  .input-list .static-fields ul .remove-static-field {
    margin-left: 5px;
  }

  .field-graph-container {
    padding-bottom: 35px;
  }

  .field-graph-container .dropdown-menu a.selected {
    font-weight: bold;
  }

  .field-graph-container .type-description {
    color: #bbb;
    font-size: 11px;
  }

  .field-graph-container .field-graph-components {
    margin-top: 10px;
    margin-right: 12px;
  }

  .field-graph-container .field-graph {
    margin-left: 40px;
    margin-bottom: 25px;
  }

  .field-graph-container .field-graph-y-axis {
    float: left;
    height: 200px;
    width: 40px;
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
    margin-top: 0px;
    margin-bottom: 0px;
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
    color: #333333;
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
    position: relative;
    left: -3px;
  }

  .dashboard .widget .reloading {
    margin-right: 2px;
    font-weight: bold;
    color: #0085A7;
    display: none;
  }

  .dashboard .widget .loading-failed {
    color: #ff4646 !important;
  }

  .dashboard .widget .controls {
    display: none;
  }

  .datatable-badge {
    border-radius: 2px;
    display: inline-block;
    padding: 5px;
    vertical-align: baseline;
  }

  .tooltip .tooltip-inner {
    max-width: 300px;
  }

  .tooltip .tooltip-inner .datapoint-info {
    text-align: left;
  }

  .tooltip .tooltip-inner .datapoint-info .date {
    color: #E3E5E5;
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
    color: #FF3B00;
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

  .dashboard .widget .quickvalues-visualization {
    overflow: auto;
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

  .message-result-fields-range {
    font-size: 11px;
  }

  .message-result-fields-set {
    margin-bottom: 10px;
  }

  /* Chief Padding Officer */
  .message-result-fields-set .btn-mini {
    padding-top: 1px;
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
    margin-bottom: 0px;
    margin-top: 0px;
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

  .node-buffer-usage {
    margin-top: 10px;
    margin-bottom: 7px;
  }

  .node-buffer-usage .progress-bar, .journal-details-usage .progress-bar {
    text-shadow: 0 1px 2px rgba(0,0,0,0.4), 2px -1px 3px rgba(255,255,255,0.5);

    span {
      margin-left: 1px;
    }
  }

  .system-system dt {
    float: left;
  }

  .system-system dd {
    margin-left: 75px;
  }

  dl.system-journal {
    margin-top: 5px;
    margin-bottom: 0px;
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

  .closed-indices {
    cursor: pointer;
  }

  .closed-indices ul {
    margin-top: 5px;
    list-style-type: square;
    margin-left: 25px;
  }

  #streamrule-form-modal .well {
    font-family: 'Open Sans', sans-serif !important;
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

  .sources th,
  .quickvalues-table th {
    background-color: #333;
    color: #fff;
    font-weight: normal;
  }

  .sources .dc-table-column._3,
  .quickvalues-table .dc-table-column._3 {
    padding-right: 0;
    text-align: right;
  }

  .dynatable-per-page-label {
    font-size: 14px;
  }

  .dynatable-search {
    font-size: 14px;
  }

  .dynatable-search input, .dynatable-per-page select {
    position: relative;
    top: 3px;
    margin-left: 3px;
  }

  .dynatable-pagination-links li:first-child {
    display: none;
  }

  .field-graph-query-container {
    position: relative;
    padding-left: 10px;
  }

  .field-graph-query {
    font-family: monospace;
    color: #9da096;
  }

  .field-graph-container .reposition-handle {
    cursor: move;
  }

  .field-graph-container .merge-hint {
    text-align: center;
    position: relative;
    top: -115px;
    margin-bottom: -21px;
    display: none;
    z-index: 2000;
  }

  .field-graph-container .merge-hint span {
    padding: 15px 50px;
    font-size: 15px;
    color: #fff;
  }

  .field-graph-container .merge-drop-ready {
    background-color: #000;
  }

  .parse-error {
    background-color: #f2dede;
    color: #a94442;
    padding-left: 2px;
    padding-right: 2px;
  }

  .field-graph-query-color {
    width: 8px;
    height: 8px;
    display: inline-block;
    margin-right: 1px;
  }

  .messages th i.sort-order-desc {
    position: relative;
    top: -1px;
  }

  .messages th i.sort-order-item {
    margin-right: 2px;
    color: #222; /* same color as .messsages th cannot display: none it because it would push the table columns out on hover :( */
  }

  .messages th i.sort-order-active,
  .messages th:hover i.sort-order-item {
    color: #fff;
  }

  .add-alert-type {
    margin-bottom: 0px;
  }

  .alert-type-form {
    font-size: 14px;
    margin-top: 15px;
  }

  .alert-type-form .help-text {
    color: #939393;
    font-size: 12px;
    margin-left: 10px;
  }

  .alert-type-form input[type=number], .alert-type-form input[type=text], .alert-type-form select {
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
    margin-bottom: 0px;
  }

  .alert-destination-form {
    display: none;
    font-size: 14px;
    margin-top: 15px;
  }

  #add-alert-receivers {
    margin-top: 15px;
  }

  #add-alert-receivers span.twitter-typeahead {
    vertical-align: middle;
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

  li.alert-condition-item:not(:last-child) {
    margin-bottom: 10px;
    border-bottom: 1px solid #ececec;
  }

  .alert-condition .in-grace {
    color: #8c8e86;
  }

  .alert-conditions hr {
    margin-top: 7px;
    margin-bottom: 7px;
  }

  .streameditpermissions, .dashboardeditpermissions {
    margin-top: 10px;
  }

  .query-exception {
    margin-bottom: 0px;
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
    // via .btn-default:hover from bootstrap
    color: #333;
    background-color: #d7d9d9;
    border-color: #c3c8c8;
    // bootstrap copy end
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
    cursor: help
  }

  .node-row .dropdown-menu a.selected {
    font-weight: bold;
  }

  #result-graph-timeline .annotation .content {
    left: -120px;
  }

  #result-graph-timeline .annotation .content:before {
    left: 117px;
  }

  .annotation .content {
    margin-bottom: 10px;
    cursor: auto !important;
  }

  .xtrc-order-handle {
    cursor: move;
    margin-right: 2px;
  }

  .xtrc-order-active {
    background-color: #00a5cf;
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
    background-color: #ffffff;
    padding: 20px;
  }

  .card .configuration-bundles .bundle-preview pre {
    background-color: #f5f5f5;
  }

  .configuration-bundles .accordion {
    background-color: #ffffff;
    border: 1px solid #ececec;
    border-radius: 2px;
  }

  .configuration-bundles .accordion-group {
    margin: 0;
    border: 0;
    border-bottom: 1px solid #ececec;
    border-radius: 0px;
    -webkit-border-radius: 0px;
    -moz-border-radius: 0px;
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
    box-shadow: 0px 1px 1px rgba(0, 0, 0, 0.05) inset;
    padding: 10px;
  }

  .configuration-bundles .bundle-preview .preview-actions form {
    display: inline-block;
    margin-left: 20px;
    margin-bottom: 0px;
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

  .bundle-preview ul, .bundle-preview ol {
    margin: 0px 0px 10px 25px;
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
    margin-bottom: 0px;
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

  table th.actions, table td.actions {
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
    padding: 15px 0px;
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
    right: 0px;
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

  ul.streams li.stream .streamrules-details {
    margin-top: 15px;
  }

  .stream-stopped {
    position: relative;
    top: -3px;
  }

  ul.entity-list {
    padding: 0;
    margin: 0;

    li.entity-list-item {
      display: block;
      padding: 15px 0px;

      h2 .label {
        margin-left: 5px;
        line-height: 2;
        vertical-align: bottom;
      }

      .item-description {
        min-height: 17px;
        margin: 5px 0;
      }

      .item-actions > .btn, .item-actions > .btn-group, .item-actions > span > .btn {
        margin-left: 5px;
        margin-bottom: 5px;
      }
    }

    li.entity-list-item:not(:last-child) {
      border-bottom: 1px solid #6dcff6;
    }
  }

  .breadcrumb {
    margin-bottom: 10px !important;
    margin-left: -15px;
    margin-right: -15px;
  }

  .graylog-node {
    margin-top: 15px;
  }

  .graylog-node-title {
    margin-top: 8px;
  }

  dl.graylog-node-state {
    margin-top: 0;
    margin-bottom: 0;
  }

  dl.graylog-node-state dt {
    float: left;
  }

  dl.graylog-node-state dd {
    margin-left: 180px;
  }

  .graylog-node .graylog-node-heap {
    margin-top: 10px;
  }

  .graylog-node .graylog-node-heap .progress {
    height: 25px;
    margin-bottom: 5px;
  }

  span.blob {
    display: inline-block;
    width: 9px;
    height: 9px;
    margin-left: 2px;
  }

  #message-table-paginator-top {
    width: 90%;
  }

  #message-table-paginator-top ul {
    margin: 0;
  }

  #message-table-paginator-bottom {
    height: 75px;
    margin-top: 20px;
  }

  #message-table-paginator-bottom nav {
    background-color: rgba(255,255,255,0.9);
    border-top: 1px #ddd solid;
    margin-left: -10px;
  }

  #message-table-paginator-bottom .affix {
    z-index: 3; // show on top of .pagination .active links
    bottom: 1px;
  }

  #message-table-paginator-bottom .affix-bottom {
    position: absolute;
  }

  #message-table-paginator-bottom .affix-bottom nav {
    border: 0;
  }

  .search-results-table {
    border-left: 2px solid #e3e3e3;
    overflow-y: auto;
    width: 100%;
  }

  .search-results-table > div {
    border-left: 1px solid #D1D1D1;
  }

  .messages-container {
    padding-right: 13px;
    width: 100%;
  }

  table.messages {
    position: relative;
    font-size: 11px;
    margin-top: 15px;
    margin-bottom: 60px;
    border-collapse: collapse;
    padding-left: 13px;
    width: 100%;
    word-break: break-all;
  }

  table.messages thead > tr {
    color: #fff;
  }

  table.messages td, table.messages th {
    position: relative;
    left: 13px;
  }

  table.messages > thead th {
    border: 0;
    font-size: 11px;
    font-weight: normal;
    background-color: #222;
    white-space: nowrap;
  }

  table.messages tr {
    border: 0 !important;
  }

  table.messages tbody.message-group {
    border-top: 0;
  }

  table.messages tbody.message-group-toggled {
    border-left: 7px solid #16ace3;
  }

  table.messages tbody.message-highlight {
    border-left: 7px solid #8dc63f;
  }

  table.messages tr.fields-row {
    cursor: pointer;
  }

  table.messages tr.fields-row td {
    padding-top: 10px;
  }

  table.messages tr.message-row td {
    border-top: 0;
    padding-top: 0;
    padding-bottom: 5px;
    font-family: monospace;
    color: #16ace3;
  }

  table.messages tr.message-row {
    margin-bottom: 5px;
    cursor: pointer;
  }

  table.messages tr.message-row .message-wrapper {
    line-height: 1.5em;
    white-space: pre-line;
    max-height: 6em; /* show 4 lines: line-height * 4 */
    overflow: hidden;
  }

  table.messages tr.message-row .message-wrapper:after {
    content: "";
    text-align: right;
    position: absolute;
    width: 99%;
    left: 5px;
    top: 4.5em;
    height: 1.5em;
    background: linear-gradient(to bottom, rgba(255, 255, 255, 0), rgba(255, 255, 255, 1) 95%);
  }

  table.messages tr.message-detail-row {
    display: none;
  }

  table.messages tr.message-detail-row td {
    padding-top: 5px;
    border-top: 0;
  }

  table.messages tr.message-detail-row .row {
    margin-right: 0;
  }

  table.messages tr.message-detail-row div[class*="col-"] {
    padding-right: 0;
  }

  .message-details-title {
    height: 30px;
  }

  .message-details-title a {
    color: #000;
  }

  .message-details-title .label {
    font-size: 50%;
    line-height: 200%;
    margin-left: 5px;
    vertical-align: bottom;
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
  .greyed-out {
    filter:alpha(opacity=50); /* IE */
    opacity: 0.5; /* Safari, Opera */
    -moz-opacity:0.50; /* FireFox */
    z-index: 20;
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
    top: 0px;
  }

  table.indexer-failures {
    margin-top: 10px;
  }

  div.row-sm {
    margin-bottom: 5px;
  }

  .graylog-node-heap .progress, .node-buffer-usage .progress, .journal-details-usage .progress {
    margin-bottom: 5px;
  }

  .journal-details-usage .progress {
    margin-top: 10px;
  }

  .journal-details-usage .progress .progress-bar {
    min-width: 3em;
  }

  .progress-bar {
    max-width: 100%;
  }

  .graylog-node-heap p {
    margin-bottom: 0px;
  }

  .graylog-node-heap .used-memory {
    background-color: #9e1f63 !important;
  }

  .graylog-node-heap .committed-memory {
    background-color: #f7941e !important;
  }

  .graylog-node-heap .max-memory {
    background-color: #f5f5f5 !important;
  }

  dl.system-system, dl.system-rest {
    margin-top: 5px;
    margin-bottom: 0px;
  }

  .table-sm {
    margin-bottom: 0px;
  }
  .graylog-input {
    margin-top: 15px;
    border-bottom: 1px
  }

  .graylog-input-actions {
    margin-top: 5px;
    text-align: right;
  }

  .graylog-input-subtitle {
    margin-top: 5px;
    margin-bottom: 0;
  }

  .graylog-input-error {
    position: relative;
    top: -4px;
    margin: 0;
    padding: 7px 7px 7px 10px;
  }

  .graylog-input-error a {
    font-weight: normal;
  }

  /* Hide the star icon... */
  .graylog-input-error i.master-node {
    display: none;
  }

  .graylog-input-metrics {
    margin-top: 5px;
  }

  .react-input-metrics {
    margin-top: 5px;
    font-size: 13px;
  }

  .input-io .value {
    font-family: monospace;
  }

  .input-io .persec {
    margin-left: 3px;
  }

  .input-io .channel-direction {
    position: relative;
    left: -1px;
  }

  .input-io .channel-direction-down {
    position: relative;
    top: 1px;
  }

  .input-io .channel-direction-up {
    position: relative;
    top: -1px;
  }

  #field-statistics th {
    cursor: pointer;
  }

  .alert-type-form {
    line-height: 26px;
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

  .no-alarm-callbacks {
    margin-top: 10px;
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

  .triggered-alerts .page-size {
    margin-top: -23px; // Height of the header
  }

  // Ensure that the stream start/pause buttons have the same size.
  .toggle-stream-button {
    width: 8.5em;
  }

  .threaddump {
    font-size: 11px;
  }

  .modal-body .form-group {
    margin-bottom: 5px;
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

  .support-sources ul {
    margin: 0;
    padding: 0;
    margin-top: 5px;
  }

  .failure-object {
    padding: 6px 10px 10px;
    margin: 10px 0 0;
    font-family: monospace;
  }

  .stream-description .fa-cube {
    margin-right: 5px;
  }

  .content-head .btn-lg {
    font-size: 16px;
  }

  .stream-loader {
    margin-top: 5px;
  }

  #field-statistics table {
    margin-top: 10px;
  }

  .quickvalues-visualization {
    padding-top: 15px;
  }

  .quickvalues-visualization .dc-chart {
    float: none;
  }

  .quickvalues-visualization .col-md-8 {
    padding-right: 0;
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

  .tag-remove:before,
  .pill-remove:before {
    content: "×";
  }

  .streamrule-connector-type-form label {
    font-size: 12px;
  }

  .streamrule-connector-type-form {
    margin-top: 10px;
    margin-bottom: 13px;
  }

  .streamrule-connector-type-form .form-group {
    margin-bottom: 5px;
  }

  .streamrule-connector-type-form .radio {
    margin-top: 0;
    margin-bottom: 0;
  }

  .streamrule-connector-type-form input[type=radio] {
    margin-top: 2px;
    margin-bottom: 2px;
  }

  #react-gettingstarted {
    margin-left: -19px;
    margin-right: -19px;
    margin-top: -4px;
  }

  #react-gettingstarted iframe {
    height: 1000px;
  }

  .sortable-list-cursor {
    cursor: move;
  }
  .sortable-list {
    .dragging {
      opacity: 0.5;
    }
    .over {
      border: 1px dashed #8c8e86;
    }

    .sortable-list-item:first-child .list-group-item {
      border-top-right-radius: 4px;
      border-top-left-radius: 4px;
    }

    .sortable-list-item:last-child .list-group-item {
      border-bottom-right-radius: 4px;
      border-bottom-left-radius: 4px;
      margin-bottom: 0;
    }

    .list-group-item:first-child {
      border-top-right-radius: 0;
      border-top-left-radius: 0;
    }

    .list-group-item:last-child {
      border-bottom-right-radius: 0;
      border-bottom-left-radius: 0;
    }
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

export default globalStyles;
