```js
const styles = ['Primary', 'Danger', 'Warning', 'Success', 'Info', 'Default'];

styles.map((style, i) => {
  return (
    <span key={`button-${style}-${i}`}>
      <Badge bsStyle={style.toLowerCase()}>{style}</Badge>{' '}
    </span>
  )
})
```
