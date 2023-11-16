import * as React from 'react';
import { Checkbox as BootstrapCheckbox } from 'react-bootstrap';

type BootstrapCheckboxProps = React.ComponentProps<typeof BootstrapCheckbox>;

type Props = Omit<BootstrapCheckboxProps, 'onChange'> & {
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void,
}
const Checkbox = ({ onChange, ...props }: Props) => <BootstrapCheckbox onChange={onChange as unknown as BootstrapCheckboxProps['onChange']} {...props} />;
export default Checkbox;
