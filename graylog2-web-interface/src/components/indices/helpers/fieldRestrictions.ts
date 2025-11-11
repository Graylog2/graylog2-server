import type { IndexSetFieldRestriction } from 'stores/indices/IndexSetsStore';

// eslint-disable-next-line import/prefer-default-export
export const parseFieldRestrictions = (field_restrictions?: IndexSetFieldRestriction[]) => {
  if (!field_restrictions || field_restrictions.length < 1) return {};

  const getHidden = () =>
    Object.keys(field_restrictions).filter(
      (field) => field_restrictions[field].filter((restriction) => restriction.type === 'hidden').length > 0,
    );

  const getImmutable = () =>
    Object.keys(field_restrictions).filter(
      (field) => field_restrictions[field].filter((restriction) => restriction.type === 'immutable').length > 0,
    );

  if (field_restrictions) return { immutableFields: getImmutable(), hiddenFields: getHidden() };

  return {};
};
