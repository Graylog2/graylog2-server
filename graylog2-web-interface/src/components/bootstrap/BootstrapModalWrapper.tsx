import React from 'react';

import Modal from './Modal';

type Props = {
  title?: string | undefined,
  backdrop?: boolean|'static'|undefined
  bsSize?: 'lg'|'large'|'sm'|'small'
  showModal: boolean,
  role?: string
  onHide: () => void,
  children: React.ReactNode,
};

const BootstrapModalWrapper = ({
  showModal,
  children,
  onHide,
  bsSize,
  backdrop = 'static',
  role = 'dialog',
  ...restProps
}: Props) => (
  <Modal show={showModal}
         onHide={onHide}
         bsSize={bsSize}
         backdrop={backdrop}
         role={role}
         {...restProps}>
    {children}
  </Modal>
);

export default BootstrapModalWrapper;
