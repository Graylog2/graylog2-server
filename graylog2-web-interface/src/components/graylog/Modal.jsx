/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
// eslint-disable-next-line no-restricted-imports
import { Modal as BootstrapModal } from 'react-bootstrap';
import styled, { css } from 'styled-components';

const Dialog = css`
  margin-top: 55px;
  
  .modal-content {
    background-color: ${({ theme }) => theme.colors.global.contentBackground};
    border-color: ${({ theme }) => theme.colors.variant.light.default};
  }
`;

const Header = css`
  border-bottom-color: ${({ theme }) => theme.colors.variant.light.default};
      
  button.close {
    color: currentColor;
  }
`;

const Title = css`
  font-size: ${({ theme }) => theme.fonts.size.h3};
`;

const Footer = css`
  border-top-color: ${({ theme }) => theme.colors.variant.light.default};
`;

const Body = css`
  .form-group {
    margin-bottom: 5px;
  }
`;

const Modal = styled(BootstrapModal)`
  .modal-backdrop {
    height: 100000%;  /* yes, really. this fixes the backdrop being cut off when the page is scrolled. */
    z-index: 1030;
  }

  form {
    margin-bottom: 0;
  }
  
  .modal-dialog {
    ${Dialog}
  }

  .modal-header {
    ${Header}
  }

  .modal-footer {
    ${Footer}
  }

  .modal-title {
    ${Title}
  }

  .modal-body {
    ${Body}
  }
`;

Modal.Dialog = styled(BootstrapModal.Dialog)`
  ${Dialog}
`;

Modal.Header = styled(BootstrapModal.Header)`
  ${Header}
`;

Modal.Title = styled(BootstrapModal.Title)`
  ${Title}
`;

Modal.Body = styled(BootstrapModal.Body)`
  ${Body}
`;

Modal.Footer = styled(BootstrapModal.Footer)`
  ${Footer}
`;

/** @component */
export default Modal;
