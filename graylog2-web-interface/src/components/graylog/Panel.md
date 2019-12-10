### Basic example

```js
// import { Panel } from 'react-bootstrap';

const PanelClickExample = () => {
  function handleClick() {
    alert('You have clicked on me');
  }

  return (
    <Panel onClick={handleClick}>
      <Panel.Body>
        Basic panel example
      </Panel.Body>
    </Panel>
  )
};

<PanelClickExample />
```

### Panel w/ Header

```js
// import { Panel } from 'react-bootstrap';

<Panel>
  <Panel.Heading>Panel heading without a title</Panel.Heading>
  <Panel.Body>Panel content</Panel.Body>
</Panel>
```

### Panel w/ Header & Title

```js
// import { Panel } from 'react-bootstrap';

<Panel>
  <Panel.Heading>
    <Panel.Title componentClass="h3">Panel heading with a title</Panel.Title>
  </Panel.Heading>
  <Panel.Body>Panel content</Panel.Body>
</Panel>
```

### Panel w/ Footer

```js
// import { Panel } from 'react-bootstrap';

<Panel>
  <Panel.Body>Panel content</Panel.Body>
  <Panel.Footer>Panel footer</Panel.Footer>
</Panel>
```

### Panel w/ Variants

```js
// import { Panel } from 'react-bootstrap';

const styles = ['Primary', 'Danger', 'Warning', 'Success', 'Info', 'Default'];

styles.map((style, i) => {
  return (
    <Panel bsStyle={style.toLowerCase()}
           key={`button-${style}-${i}`}>
      <Panel.Heading>{`${style} Heading`}</Panel.Heading>
      <Panel.Body>Panel content</Panel.Body>
    </Panel>
  )
})
```

### Collapsible Panel

```js
import { Button } from 'components/graylog';

const PanelCollapseExample = () => {
   const [expanded, setExpanded] = React.useState(false);

  return (
    <div>
      <Button onClick={() => setExpanded(!expanded)}>
        Click to toggle
      </Button>

      <Panel expanded={expanded}>
        <Panel.Collapse>
          <Panel.Body>
            Anim pariatur cliche reprehenderit, enim eiusmod high life
            accusamus terry richardson ad squid. Nihil anim keffiyeh
            helvetica, craft beer labore wes anderson cred nesciunt sapiente
            ea proident.
          </Panel.Body>
        </Panel.Collapse>
      </Panel>
    </div>
  );
};

<PanelCollapseExample />
```

## Deprecated Examples

### Deprecated Basic example

```js
const DeprecatedPanelClickExample = () => {
  function handleClick() {
    alert('You have clicked on me');
  }

  return (
    <Panel onClick={handleClick}>Click me Example</Panel>
  )
};

<DeprecatedPanelClickExample />
```

### Deprecated Panel w/ Header

```js
<Panel header="Panel heading without a title">
  Panel content
</Panel>
```

### Deprecated Panel w/ Footer

```js
<Panel footer="Panel footer">
  Panel content
</Panel>
```


### Deprecated Panel w/ Variants

```js
const styles = ['Primary', 'Danger', 'Warning', 'Success', 'Info', 'Default'];

styles.map((style, i) => {
  return (
    <Panel bsStyle={style.toLowerCase()}
           key={`button-${style}-${i}`}
           header={`${style} Heading`}>
      Lorem ipsum dolor sit amet consectetur adipisicing elit.
    </Panel>
  )
})
```

### Deprecated Collapsible Panel

```js
import { Button } from 'components/graylog';

const DeprecatedPanelCollapseExample = () => {
   const [expanded, setExpanded] = React.useState(false);

  return (
    <div>
      <Button onClick={() => setExpanded(!expanded)}>
        Click to togle
      </Button>
      <Panel collapsible expanded={expanded}>
        Anim pariatur cliche reprehenderit, enim eiusmod high life accusamus terry richardson ad squid.
        Nihil anim keffiyeh helvetica, craft beer labore wes anderson cred nesciunt sapiente ea proident.
      </Panel>
    </div>
  )
};

<DeprecatedPanelCollapseExample />
```
