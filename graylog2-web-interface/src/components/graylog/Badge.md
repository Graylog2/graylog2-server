```js
const styles = ['Primary', 'Danger', 'Warning', 'Success', 'Info', 'Default'];

styles.map((style, i) => {
  return (
    <>
      <Badge bsStyle={style.toLowerCase()} key={`button-${style}-${i}`}>{style}</Badge>{' '}
    </>
  )
})
```
