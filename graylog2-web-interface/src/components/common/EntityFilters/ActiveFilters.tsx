import React from 'react';

import type { Filters } from 'components/common/EntityFilters/types';
import type { Attributes } from 'stores/PaginationTypes';

type Props = {
  filters: Filters
  attributes: Attributes
}

const ActiveFilters = ({ attributes = [], filters }: Props) => {
  return (
    <div>
      {Object.entries(filters).map(([attributeId, filterValues]) => {
        const relatedAttribute = attributes.find(({ id }) => id === attributeId);

        return (
          <div>
            {relatedAttribute.title}
            {filterValues.map(({ value, title }) => {
              return <div>{title}</div>;
            })}
          </div>
        );
      })}

    </div>
  );
};

export default ActiveFilters;
