// eslint-disable-next-line no-restricted-imports
import { Modal as BootstrapModal } from 'react-bootstrap';
import styled from 'styled-components';
import { transparentize } from 'polished';

import teinte from 'theme/teinte';

const Modal = styled(BootstrapModal)`
  .modal-content {
    background-color: ${teinte.primary.due};
    border-color: ${transparentize(0.8, teinte.primary.tre)};

    .modal-header {
      border-bottom-color: ${teinte.secondary.tre};
    }

    .modal-footer {
      border-top-color: ${teinte.secondary.tre};
    }
  }
`;

/** @component */
export default Modal;
