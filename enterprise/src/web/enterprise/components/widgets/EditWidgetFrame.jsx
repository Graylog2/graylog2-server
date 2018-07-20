import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import { Modal } from 'react-bootstrap';

import styles from '!style?insertAt=bottom!css!./EditWidgetFrame.css';

const EditWidgetDialog = ({ children, ...rest }) => <Modal.Dialog {...rest} dialogClassName={styles.editWidgetDialog}>{children}</Modal.Dialog>;

EditWidgetDialog.propTypes = {
  children: PropTypes.node.isRequired,
};

const EditWidgetFrame = createReactClass({
  propTypes: {
    children: PropTypes.node.isRequired,
  },

  render() {
    return (
      <Modal show animation={false} dialogComponentClass={EditWidgetDialog} enforceFocus={false}>
        <div style={{ height: 'calc(100% - 20px)' }}>
          {this.props.children}
        </div>
      </Modal>
    );
  },
});

export default EditWidgetFrame;
