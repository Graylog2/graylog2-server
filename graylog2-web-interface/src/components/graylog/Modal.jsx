// eslint-disable-next-line no-restricted-imports
import { Modal as BootstrapModal } from 'react-bootstrap';
import styled from 'styled-components';
import { transparentize } from 'polished';

import { color } from 'theme';

const Modal = styled(BootstrapModal)`
  .modal-content {
    background-color: ${color.primary.due};
    border-color: ${transparentize(0.8, color.primary.tre)};

    .modal-header {
      border-bottom-color: ${color.secondary.tre};
    }

    .modal-footer {
      border-top-color: ${color.secondary.tre};
    }
  }
`;

export default Modal;
