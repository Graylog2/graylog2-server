## Form & Modal Submit Buttons

- Rely on the shared components `FormSubmit` and `ModalSubmit` to implement the submit and cancel button.
  The `FormSubmit` can be used for all forms on pages. The `ModalSubmit` can be used for modals and similar element like 
  popovers or the login dialog.
- Make sure to follow the placements defined by these shared components. Currently:
  - submit buttons in form are vertically aligned with the inputs. The cancel button is being placed after the submit button.
  - submit buttons in modals are always placed in the right bottom corner. The cancel button is being placed before the submit button.
- When defining a name for the submit button
  - Instead of `Save` or `Ok` use a meaningful name for the submit button like `Create stream`.
  - Make sure to write only the first letter uppercase and all other letter lowercase. 
- Always use `Cancel` for the cancel button name.

## `EmptyEntity` & `NoEntitiesExist` & `NoSearchResults` components

- These three components are closely related and maybe confusing to decide which one to use for which situation.
  - `EmptyEntity` should be used to display a message for an entity that does not have any entries in the database yet. This components supports displaying a message explaining what the entity is and can also support including a button link to create one via the children props
  - `NoEntitiesExist` is similar to `EmptyEntity` except it is a more generic message without including the option to create a new one
  - `NoSearchResults` should be used in the case when a search is performed with a query and the result is no matching entries. This component should inform the user that entities still exist, just that none match the current filter.
