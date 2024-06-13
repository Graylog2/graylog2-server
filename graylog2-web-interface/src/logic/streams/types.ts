export type Stream = {
  id: string,
  creator_user_id: string,
  outputs: any[],
  matching_type: 'AND' | 'OR',
  description: string,
  created_at: string,
  disabled: boolean,
  rules: StreamRule[],
  alert_conditions?: any[],
  alert_receivers?: {
    emails: Array<string>,
    users: Array<string>,
  },
  title: string,
  content_pack: any,
  remove_matches_from_default_stream: boolean,
  index_set_id: string,
  is_default: boolean,
  is_editable: boolean,
};

export type StreamRule = {
  id: string,
  type: number,
  value: string,
  field: string,
  inverted: boolean,
  stream_id: string,
  description: string,
};
