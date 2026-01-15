export const createStreamFixture = (
  identifier: string,
  defaultStream: boolean = false,
  editable: boolean = true,
  matchingType: 'AND' | 'OR' = 'OR',
) => ({
  id: `stream-id-${identifier}`,
  creator_user_id: `stream-creator-id-${identifier}`,
  outputs: [],
  matching_type: matchingType,
  description: `Stream Description ${identifier}`,
  created_at: new Date().toISOString(),
  disabled: false,
  rules: [],
  title: `Stream Title ${identifier}`,
  content_pack: undefined,
  remove_matches_from_default_stream: false,
  index_set_id: `index-set-id-${identifier}`,
  is_default: defaultStream,
  is_editable: editable,
  categories: [],
});

export default createStreamFixture;
