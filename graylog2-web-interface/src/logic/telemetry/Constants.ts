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
  // Event Definitions
  EVENTDEFINITION_CREATE_BUTTON_CLICKED: 'EventDefinition Create Button Clicked',
  EVENTDEFINITION_DETAILS_STEP_CLICKED: 'EventDefinition Details Step Clicked',
  EVENTDEFINITION_CONDITION_STEP_CLICKED: 'EventDefinition Condition Step Clicked',
  EVENTDEFINITION_FIELDS_STEP_CLICKED: 'EventDefinition Fields Step Clicked',
  EVENTDEFINITION_NOTIFICATIONS_STEP_CLICKED: 'EventDefinition Notifications Step Clicked',
  EVENTDEFINITION_SUMMARY_STEP_CLICKED: 'EventDefinition Summary Step Clicked',
  EVENTDEFINITION_NEXT_CLICKED: 'EventDefinition Next Clicked',
  EVENTDEFINITION_PREVIOUS_CLICKED: 'EventDefinition Previous Clicked',
  EVENTDEFINITION_PRIORITY_CHANGED: 'EventDefinition Priority Changed',
  EVENTDEFINITION_CONDITION_TYPE_SELECTED: 'EventDefinition Condition Type Selected',
  EVENTDEFINITION_STREAM_SELECTED: 'EventDefinition Stream Selected',
  EVENTDEFINITION_SEARCH_WITHIN_THE_LAST_UNIT_CHANGED: 'EventDefinition Search Within The Last Unit Changed',
  EVENTDEFINITION_EXECUTE_SEARCH_EVERY_UNIT_CHANGED: 'EventDefinition Execute Search Every Unit Changed',
  EVENTDEFINITION_EXECUTED_AUTOMATICALLY_TOGGLED: 'EventDefinition Executed Automatically Toggled',
  EVENTDEFINITION_EVENT_LIMIT_CHANGED: 'EventDefinition Event Limit Changed',
  EVENTDEFINITION_AGGREGATION_TOGGLED: 'EventDefinition Aggregation Toggled',
  EVENTDEFINITION_AGGREGATION_GROUP_BY_FIELD_SELECTED: 'EventDefinition Aggregation Group By Field Selected',
  EVENTDEFINITION_ADD_CUSTOM_FIELD_CLICKED: 'EventDefinition Add Custom Field Clicked',
  EVENTDEFINITION_FIELD_AS_EVENT_KEY_TOGGLED: 'EventDefinition Field As Event Key Toggled',
  EVENTDEFINITION_SET_VALUE_FROM_TEMPLATE_SELECTED: 'EventDefinition Set Value From Template Selected',
  EVENTDEFINITION_SET_VALUE_FROM_LOOKUP_TABLE_SELECTED: 'EventDefinition Set Value From Lookup Table Selected',
  EVENTDEFINITION_ADD_NOTIFICATION_CLICKED: 'EventDefinition Add Notification Clicked',
  EVENTDEFINITION_MANAGE_NOTIFICATIONS_LINK_CLICKED: 'EventDefinition Manage Notifications Link Clicked',
  EVENTDEFINITION_NOTIFICATION_SELECTED: 'EventDefinition Notification Selected',
  EVENTDEFINITION_CREATE_NEW_NOTIFICATION_SELECTED: 'EventDefinition Create New Notification Selected',
  EVENTDEFINITION_NOTIFICATION_TYPE_SELECTED: 'EventDefinition Notification Type Selected',
  EVENTDEFINITION_CREATION_SUMMARY_CANCEL_BUTTON_CLICKED: 'EventDefinition Creation Summary Cancel Button Clicked',
  EVENTDEFINITION_CREATION_SUMMARY_CREATE_BUTTON_CLICKED: 'EventDefinition Creation Summary Create Button Clicked',
} as const;
type ObjectValues<T> = T[keyof T];

export type EventType = ObjectValues<typeof TELEMETRY_EVENT_TYPE>
