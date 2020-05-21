// eslint-disable-next-line no-restricted-imports
import { Modal as BootstrapModal } from 'react-bootstrap';
import styled, { css } from 'styled-components';
import chroma from 'chroma-js';

const Modal = styled(BootstrapModal)(({ theme }) => css`
  .modal-content {
    background-color: ${theme.colors.global.contentBackground};
    border-color: ${chroma(theme.colors.gray[10]).alpha(0.2).css()};

    .modal-header {
      border-bottom-color: ${theme.colors.gray[90]};
    }

    .modal-footer {
      border-top-color: ${theme.colors.gray[90]};
    }
  }

  .modal-title {
    font-size: 21px;
  }

  .modal-dialog {
    margin-top: 55px;
  }

  .modal-backdrop {
    height: 100000%;  /* yes, really. this fixes the backdrop being cut off when the page is scrolled. */
    z-index: 1030;
  }

  form {
    margin-bottom: 0;
  }

  .modal-body .form-group {
    margin-bottom: 5px;
  }
`);

/** @component */
export default Modal;
