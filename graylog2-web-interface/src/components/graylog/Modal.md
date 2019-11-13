```js
import { Button } from 'components/graylog';

const ModalExample = () => {
  const [show, setShow] = React.useState(false);
  const toggleShow = () => setShow(!show);

  return (
    <div>
      <Button onClick={toggleShow}>Open Modal</Button>

      <Modal show={show} onHide={toggleShow}>
        <Modal.Header closeButton>
          <Modal.Title>Lorem Ipsum Presents:</Modal.Title>
        </Modal.Header>

        <Modal.Body>
          <p><strong>Pellentesque habitant morbi tristique</strong> senectus et netus et malesuada fames ac turpis egestas. Vestibulum tortor quam, feugiat vitae, ultricies eget, tempor sit amet, ante. Donec eu libero sit amet quam egestas semper. <em>Aenean ultricies mi vitae est.</em> Mauris placerat eleifend leo. Quisque sit amet est et sapien ullamcorper pharetra. Vestibulum erat wisi, condimentum sed, <code>commodo vitae</code>, ornare sit amet, wisi. Aenean fermentum, elit eget tincidunt condimentum, eros ipsum rutrum orci, sagittis tempus lacus enim ac dui. <a href="#">Donec non enim</a> in turpis pulvinar facilisis. Ut felis.</p>
        </Modal.Body>

        <Modal.Footer>
          <Button type="button" bsStyle="danger" onClick={toggleShow}>Close</Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
};

<ModalExample />
```
