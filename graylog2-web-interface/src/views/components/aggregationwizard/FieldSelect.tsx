import * as React from 'react';
import { useContext } from 'react';
import Immutable from 'immutable';

import FieldSelectBase from 'views/components/aggregationwizard/FieldSelectBase';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';

type Props = Omit<React.ComponentProps<typeof FieldSelectBase>, 'options'>

const FieldSelect = (props: Props) => {
  const activeQuery = useActiveQueryId();
  const fieldTypes = useContext(FieldTypesContext);
  const fieldOptions = fieldTypes.queryFields.get(activeQuery, Immutable.List()).toArray();

  return (
    <FieldSelectBase options={fieldOptions}
                     {...props} />
  );
};

export default FieldSelect;
