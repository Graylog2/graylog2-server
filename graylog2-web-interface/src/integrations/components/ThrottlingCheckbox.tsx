import * as React from 'react';
import useProductName from 'brand-customization/useProductName';

import { Input } from 'components/bootstrap';

type Props = {
  id: string;
  defaultChecked: boolean;
  onChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
};

const ThrottlingCheckbox = ({ id, defaultChecked, onChange }: Props) => {
  const productName = useProductName();

  return (
    <Input
      id={id}
      type="checkbox"
      value="enable-throttling"
      defaultChecked={defaultChecked}
      onChange={onChange}
      label="Enable Throttling"
      help={`If enabled, no new messages will be read from this input until the ${productName} server catches up with its message load. This is typically useful for inputs reading from files or message queue systems like AMQP or Kafka. If you regularly poll an external system, e.g. via HTTP, you normally want to leave this disabled.`}
    />
  );
};

export default ThrottlingCheckbox;
