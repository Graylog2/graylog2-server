import * as React from 'react';

import useEventDefinition from 'hooks/useEventDefinition';
import Spinner from 'components/common/Spinner';

const EventDefinition = ({ value }: { value: string }) => {
  const { data, isLoading } = useEventDefinition(value);

  if (isLoading) {
    return <Spinner />;
  }

  const { eventDefinition } = data;

  return <span>{eventDefinition?.title ?? value}</span>;
};

export default EventDefinition;
