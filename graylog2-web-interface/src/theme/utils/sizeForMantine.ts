import type { BsSize } from 'components/bootstrap/types';

const sizeForMantine = (size: BsSize) => {
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
