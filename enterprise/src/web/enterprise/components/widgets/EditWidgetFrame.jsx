import React from 'react';
import PropTypes from 'prop-types';
import { Modal } from 'react-bootstrap';

import styles from '!style?insertAt=bottom!css!./EditWidgetFrame.css';

const EditWidgetDialog = ({ children, ...rest }) => <Modal.Dialog {...rest} dialogClassName={styles.editWidgetDialog}>{children}</Modal.Dialog>;

const EditWidgetFrame = ({ children }) => (
  <Modal show animation={false} dialogComponentClass={EditWidgetDialog} enforceFocus={false}>
    <div style={{ height: 'calc(100% - 20px)' }}>
      {children}
    </div>
  </Modal>
);

EditWidgetFrame.propTypes = {};

export default EditWidgetFrame;
