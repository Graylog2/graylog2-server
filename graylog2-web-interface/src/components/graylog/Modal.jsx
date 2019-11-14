// eslint-disable-next-line no-restricted-imports
import { Modal as BootstrapModal } from 'react-bootstrap';
import styled from 'styled-components';
import { transparentize } from 'polished';

import { color } from 'theme';

const Modal = styled(BootstrapModal)`
  .modal-content {
    background-color: ${color.global.contentBackground};
    border-color: ${transparentize(0.8, color.gray[0])};

    .modal-header {
      border-bottom-color: ${color.gray[90]};
    }

    .modal-footer {
      border-top-color: ${color.gray[90]};
    }
  }
`;

export default Modal;
