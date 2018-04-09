import PropTypes from 'prop-types';
import React from 'react';

import { Button } from 'react-bootstrap';
import ContentPackDetails from 'components/content-packs/ContentPackDetails';

class ContentPackPreview extends React.Component {
  static propTypes = {
    contentPack: PropTypes.object.isRequired,
    onSave: PropTypes.func,
  };

  static defaultProps = {
    onSave: () => {},
  };

  render() {
    return (
      <div>
        <ContentPackDetails contentPack={this.props.contentPack} />
        <Button id="create" onClick={this.props.onSave}>Create</Button>
      </div>
    );
  }
}

export default ContentPackPreview;
