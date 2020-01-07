// eslint-disable-next-line no-restricted-imports
import { Modal as BootstrapModal } from 'react-bootstrap';
import styled, { css } from 'styled-components';
import { transparentize } from 'polished';

const Modal = styled(BootstrapModal)(({ theme }) => css`
  .modal-content {
    background-color: ${theme.color.global.contentBackground};
    border-color: ${transparentize(0.8, theme.color.gray[0])};

    .modal-header {
      border-bottom-color: ${theme.color.gray[90]};
    }

    .modal-footer {
      border-top-color: ${theme.color.gray[90]};
    }
  }
`);

export default Modal;
