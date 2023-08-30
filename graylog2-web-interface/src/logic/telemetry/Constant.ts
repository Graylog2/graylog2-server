export const TELEMETRY_EVENT_TYPE = {
  SEARCH_TIMERANGE_PRESET_SELECTED: 'Search TimeRange Preset Selected',
  SEARCH_TIMERANGE_PICKER_TOGGLED: 'Search TimeRange Picker Toggled',
  SEARCH_STREAM_INPUT_CHANGED: 'Search Stream Input Changed',
  SEARCH_REFRESH_CONTROL_PRESET_SELECTED: 'Search Refresh Control Preset Selected',
  SEARCH_REFRESH_CONTROL_TOGGLED: 'Search Refresh Control Toggled',
  SEARCH_BUTTON_CLICKED: 'Search Button Clicked',
} as const;
type ObjectValues<T> = T[keyof T];

export type EventType = ObjectValues<typeof TELEMETRY_EVENT_TYPE>
