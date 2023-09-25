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

export const TELEMETRY_EVENT_TYPE = {
  SEARCH_TIMERANGE_PRESET_SELECTED: 'Search TimeRange Preset Selected',
  SEARCH_TIMERANGE_PICKER_TOGGLED: 'Search TimeRange Picker Toggled',
  SEARCH_TIMERANGE_PICKER_UPDATED: 'Search TimeRange Picker Updated',
  SEARCH_TIMERANGE_PICKER_CANCELED: 'Search TimeRange Picker Canceled',
  SEARCH_TIMERANGE_PICKER_TAB_SELECTED: 'Search TimeRange Picker Tab Selected',
  SEARCH_STREAM_INPUT_CHANGED: 'Search Stream Input Changed',
  SEARCH_REFRESH_CONTROL_PRESET_SELECTED: 'Search Refresh Control Preset Selected',
  SEARCH_REFRESH_CONTROL_TOGGLED: 'Search Refresh Control Toggled',
  SEARCH_BUTTON_CLICKED: 'Search Button Clicked',
  SEARCH_FILTER_CREATE_CLICKED: 'Search Filter Create Clicked',
  SEARCH_FILTER_LOAD_CLICKED: 'Search Filter Load Clicked',
  SEARCH_FILTER_LOADED: 'Search Filter Loaded',
  SEARCH_FILTER_ITEM_MENU_TOGGLED: 'Search Filter Item Menu Toggled',
  SEARCH_FILTER_ITEM_DISABLED_TOGGLED: 'Search Filter Item Disabled Toggled',
  SEARCH_FILTER_ITEM_SHARE_CLICKED: 'Search Filter Item Share Clicked',
  SEARCH_FILTER_ITEM_EDIT_CLICKED: 'Search Filter Item Edit Clicked',
  SEARCH_FILTER_ITEM_COPIED: 'Search Filter Item Copied',
  SEARCH_FILTER_ITEM_REMOVED: 'Search Filter Item Removed',
  SEARCH_FILTER_ITEM_REFERENCE_REPLACED: 'Search Filter Item Reference Replaced',
  SEARCH_FILTER_ITEM_NEGATION_TOGGLED: 'Search Filter Item Negation Toggled',
  SEARCH_WIDGET_EXPORT_DOWNLOADED: 'Search Widget Export Downloaded',
  SEARCH_WIDGET_ACTION: {
    FOCUSED: 'Search Widget Focused',
    DELETED: 'Search Widget Deleted',
    MOVE: 'Search Widget Moved',
    DUPLICATE: 'Search Widget Duplicate',
    COPY_TO_DASHBOARD: 'Search Widget Copy To Dashboard Clicked',
    SEARCH_WIDGET_HORIZONTAL_STRETCH: 'Search Widget Horizontal Width Toggled',
    SEARCH_WIDGET_EXTRA_ACTION: 'Search Widget Extra Action Clicked',
    WIDGET_EDIT_TOGGLED: 'Search Widget Edit Toggled',
    WIDGET_EDIT_CANCEL_CLICKED: 'Search Widget Edit Cancel Clicked',
    WIDGET_CONFIG_UPDATED: 'Search Widget Config Updated',
  },
  SEARCH_WIDGET_CREATE: {
    AGGREGATION: 'Search Widget Aggregation Created',
    MESSAGE_COUNT: 'Search Widget Message Count Created',
    MESSAGE_TABLE: 'Search Widget Message Table Created',
    LOG_VIEW: 'Search Widget Log View Created',
  },
  SEARCH_FIELD_VALUE_ACTION: {
    CHART: 'Search Field Action Chart Clicked',
    STATISTICS: 'Search Field Action Statistics Clicked',
    SHOW_TOP_VALUES: 'Search Field Action Show Top Values Clicked',
    ADD_TO_TABLE: 'Search Field Action Add To Table Clicked',
    REMOVE_FROM_TABLE: 'Search Field Action Remove From Table Clicked',
    ADD_TO_ALL_TABLES: 'Search Field Action Add To All Tables Clicked',
    COPY_FIELD_NAME_TO_CLIPBOARD: 'Search Field Action Copy Field Name To Clipboard Clicked',
    REMOVE_FROM_ALL_TABLES: 'Search Field Action Remove From All Tables Clicked',
    EXCLUDE_FROM_RESULTS: 'Search Value Action Exclude From Results Clicked',
    ADD_TO_QUERY: 'Search Value Action Add To Query Clicked',
    SHOW_DOCUMENT_FOR_VALUE: 'Search Value Action Show Document For Value Clicked',
    CREATE_EXTRACTOR: 'Search Value Action Create Extractor Clicked',
    HIGHLIGHT_THIS_VALUE: 'Search Value Action Highlight This Value Clicked',
    COPY_VALUE_TO_CLIPBOARD: 'Search Value Action Copy Value To Clipboard Clicked',
    CREATE_EVENT_DEFINITION: 'Search Value Action Create Event Definition Clicked',
    INSERT_INTO_DASHBOARD_SEARCH: 'Search Value Action Insert Into Dashboard/Search Clicked',
  },
  DASHBOARD_FULL_SCREEN_MODE_STARTED: 'Dashboard Fullscreen Mode Started',
  SEARCH_MESSAGE_TABLE_SHOW_SURROUNDING_MESSAGE: 'Search Message Table Show Surrounding Message Clicked ',
  SEARCH_MESSAGE_TABLE_TEST_AGAINST_STREAM: 'Search Message Table Test Against Stream Clicked ',
  SEARCH_MESSAGE_TABLE_DETAILS_TOGGLED: 'Widget Message Table Details Toggled',
  SEARCH_SIDEBAR_TOGGLE: 'Search Sidebar Toggled',
  SEARCH_SIDEBAR_HIGHLIGHT_CREATED: 'Search Sidebar Highlight Created',
  SEARCH_SIDEBAR_HIGHLIGHT_UPDATED: 'Search Sidebar Highlight Updated',
  SEARCH_SIDEBAR_HIGHLIGHT_DELETED: 'Search Sidebar Highlight Deleted',
  SEARCH_SIDEBAR_REDO: 'Search Sidebar Redo',
  SEARCH_SIDEBAR_UNDO: 'Search Sidebar Undo',
  DASHBOARD_ACTION: {
    DASHBOARD_NEW_SAVED: 'Dashboard New Saved',
    DASHBOARD_SAVED: 'Dashboard Saved',
    DASHBOARD_UPDATED: 'Dashboard Updated',
    DASHBOARD_CREATE_CLICKED: 'Dashboard Create Clicked',
    DASHBOARD_CREATE_PAGE: 'Dashboard Create Page Clicked',
    DASHBOARD_PAGE_CONFIGURATION: 'Dashboard Page Configuration Clicked',
    DASHBOARD_PAGE_CONFIGURATION_UPDATED: 'Dashboard Page Configuration Updated',
    DASHBOARD_PAGE_CONFIGURATION_CANCELED: 'Dashboard Page Configuration Update Cancel',
    DASHBOARD_PAGE_CONFIGURATION_SORTING_UPDATED: 'Dashboard Page Configuration Sorting Updated',
    DASHBOARD_PAGE_CONFIGURATION_PAGE_REMOVED: 'Dashboard Page Configuration Page Remove',
  },
  EVENTDEFINITION_CREATE_BUTTON_CLICKED: 'EventDefinition Create Button Clicked',
  EVENTDEFINITION_NEXT_CLICKED: 'EventDefinition Next Clicked',
  EVENTDEFINITION_PREVIOUS_CLICKED: 'EventDefinition Previous Clicked',
  EVENTDEFINITION_DETAILS: {
    STEP_CLICKED: 'EventDefinition Details Step Clicked',
    PRIORITY_CHANGED: 'EventDefinition Details Priority Changed',
  },
  EVENTDEFINITION_CONDITION: {
    STEP_CLICKED: 'EventDefinition Condition Step Clicked',
    TYPE_SELECTED: 'EventDefinition Condition Type Selected',
    FILTER_STREAM_SELECTED: 'EventDefinition Condition Filter Stream Selected',
    FILTER_SEARCH_WITHIN_THE_LAST_UNIT_CHANGED: 'EventDefinition Condition Filter Search Within The Last Unit Changed',
    FILTER_EXECUTE_SEARCH_EVERY_UNIT_CHANGED: 'EventDefinition Condition Filter Execute Search Every Unit Changed',
    FILTER_EXECUTED_AUTOMATICALLY_TOGGLED: 'EventDefinition Condition Filter Executed Automatically Toggled',
    FILTER_EVENT_LIMIT_CHANGED: 'EventDefinition Condition Filter Event Limit Changed',
    AGGREGATION_TOGGLED: 'EventDefinition Aggregation Toggled',
    AGGREGATION_GROUP_BY_FIELD_SELECTED: 'EventDefinition Aggregation Group By Field Selected',
    CORRELATION_WITHIN_THE_LAST_UNIT_CHANGED: 'EventDefinition Condition Correlation Within The Last Unit Changed',
    CORRELATION_EXECUTE_EVERY_UNIT_CHANGED: 'EventDefinition Condition Correlation Execute Every Unit Changed',
    CORRELATION_EXECUTED_AUTOMATICALLY_TOGGLED: 'EventDefinition Condition Correlation Executed Automatically Toggled',
    CORRELATION_EVENT_SELECTED: 'EventDefinition Condition Correlation Event Selected',
    CORRELATION_ADD_EVENT_CLICKED: 'EventDefinition Condition Correlation Add Event Clicked',
    CORRELATION_REMOVE_EVENT_CLICKED: 'EventDefinition Condition Correlation Remove Event Clicked',
    CORRELATION_OCCURRENCE_TIME_CHANGED: 'EventDefinition Condition Correlation Occurrence Time Changed',
    CORRELATION_NO_OCCURRENCE_FOLLOWED_BY_EVENT_SELECTED: 'EventDefinition Condition Correlation No Occurrence Followed By Event Selected',
    CORRELATION_NO_OCCURRENCE_IN_THE_NEXT_SELECTED: 'EventDefinition Condition Correlation No Occurrence In The Next Selected',
    CORRELATION_NO_OCCURRENCE_IN_THE_NEXT_UNIT_CHANGED: 'EventDefinition Condition Correlation No Occurrence In The Next Unit Changed',
  },
  EVENTDEFINITION_FIELDS: {
    STEP_CLICKED: 'EventDefinition Fields Step Clicked',
    ADD_CUSTOM_FIELD_CLICKED: 'EventDefinition Fields Add Custom Field Clicked',
    AS_EVENT_KEY_TOGGLED: 'EventDefinition Fields As Event Key Toggled',
    SET_VALUE_FROM_TEMPLATE_SELECTED: 'EventDefinition Fields Set Value From Template Selected',
    SET_VALUE_FROM_LOOKUP_TABLE_SELECTED: 'EventDefinition Fields Set Value From Lookup Table Selected',
    DONE_CLICKED: 'EventDefinition Fields Done Clicked',
    CANCEL_CLICKED: 'EventDefinition Fields Cancel Clicked',
  },
  EVENTDEFINITION_NOTIFICATIONS: {
    STEP_CLICKED: 'EventDefinition Notifications Step Clicked',
    ADD_CLICKED: 'EventDefinition Notifications Add Clicked',
    MANAGE_LINK_CLICKED: 'EventDefinition Notifications Manage Link Clicked',
    NOTIFICATION_SELECTED: 'EventDefinition Notifications Notification Selected',
    CREATE_NEW_CLICKED: 'EventDefinition Notifications Create New Clicked',
    NOTIFICATION_TYPE_SELECTED: 'EventDefinition Notifications Notification Type Selected',
    DONE_CLICKED: 'EventDefinition Notifications Done Clicked',
    CANCEL_CLICKED: 'EventDefinition Notifications Cancel Clicked',
  },
  EVENTDEFINITION_SUMMARY: {
    STEP_CLICKED: 'EventDefinition Summary Step Clicked',
    CANCEL_CLICKED: 'EventDefinition Summary Cancel Clicked',
    CREATE_CLICKED: 'EventDefinition Summary Create Clicked',
    UPDATE_CLICKED: 'EventDefinition Summary Update Clicked',
  },
} as const;

type ExtractObjectValues<T extends object> = {
  [K in keyof T]: T[K] extends object ? ExtractObjectValues<T[K]> : T[K];
}[keyof T];

export type EventType = ExtractObjectValues<typeof TELEMETRY_EVENT_TYPE>;
