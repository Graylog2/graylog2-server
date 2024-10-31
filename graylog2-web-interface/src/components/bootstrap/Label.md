```js { "props": { "className": "container" } }
const styles = ['Primary', 'Danger', 'Warning', 'Success', 'Info', 'Default'];

<table className="table table-sm table-striped">
  <tbody>
    {styles.map((style, i) => (
      <tr key={`button-${style}-${i}`}>
        <th><strong>{style}:</strong></th>
        <td>
          <Label bsStyle={style.toLowerCase()}>
            EXAMPLE
          </Label>
        </td>
      </tr>
    ))}
  </tbody>
</table>
```
