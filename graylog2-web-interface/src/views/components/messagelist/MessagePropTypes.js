// @flow strict
/* eslint-disable import/prefer-default-export */
import { shape, object, string } from 'prop-types';

export const Message = shape({
  fields: object.isRequired,
  highlight_ranges: object,
  id: string.isRequired,
  index: string.isRequired,
  decoration_stats: shape({
    added_fields: object,
    changed_fields: object,
    removed_fields: object,
  }),
});
