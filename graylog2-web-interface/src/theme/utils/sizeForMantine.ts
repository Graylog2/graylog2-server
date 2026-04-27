import type { BsSize } from 'components/bootstrap/types';
import type { SupportedMantineSize } from 'theme/types';

const sizeForMantine = (size: BsSize): SupportedMantineSize => {
  switch (size) {
    case 'xs':
    case 'xsmall':
      return 'xs';
    case 'sm':
    case 'small':
      return 'sm';
    case 'lg':
    case 'large':
      return 'lg';
    default:
      return 'md';
  }
};

export default sizeForMantine;
