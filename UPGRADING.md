Upgrading to Graylog 6.2.x
==========================

## Breaking Changes

### Plugins
* This release includes Java API changes which might require plugin authors to adjust their code. Please check
  [Java API Changes](#java-api-changes) for details.
* Adjustment of `enterpriseWidgets` web interface plugin. The `editComponent` attribute now no longer has a `onSubmit` prop.
  Before this change the prop had to be called to close the widget edit mode. Now it is enough to call `applyAllWidgetChanges` from the `WidgetEditApplyAllChangesContext`.
  Alternatively the `SaveOrCancelButtons` component can be used in the edit component for custom widgets. It renders a cancel and submit button and calls `applyAllWidgetChanges` on submit.

## Configuration File Changes

| Option        | Action     | Description                                    |
|---------------|------------|------------------------------------------------|
| `tbd`         | **added**  |                                                |

## Default Configuration Changes

- tbd

## Java API Changes

Upgraded [MongoJack](https://github.com/mongojack/mongojack) to version 5.x. This impacts the Java API for accessing
documents in MongoDB. Some previously deprecated MongoJack classes (like `org.mongojack.DBQuery`) have been removed.
Plugin authors will have to replace usages of removed classes to corresponding classes from the MongoDB driver
packages, most prominently `com.mongodb.client.model.Filters`.

Additionally, the following Java Code API changes are included in this release:

| File/method                                                                       | Description                              |
|-----------------------------------------------------------------------------------|------------------------------------------|
| `org.graylog.scheduler.JobSchedule#toDBUpdate`                                    | removed                                  |
| `org.graylog.scheduler.DBJobTriggerService#all`                                   | replaced by streamAll                    |
| `org.graylog.scheduler.DBJobTriggerService#getAllForJob`                          | replaced by streamAllForJob              |
| `org.graylog.scheduler.DBJobTriggerService#findByQuery`                           | replaced by streamByQuery                |
| `org.graylog.events.processor.DBEventDefinitionService#getByNotificationId`       | replaced by streamByNotificationId       |
| `org.graylog.events.processor.DBEventDefinitionService#getSystemEventDefinitions` | replaced by streamSystemEventDefinitions |
| `org.graylog.events.processor.DBEventDefinitionService#getByArrayValue`           | replaced by streamByArrayValue           |
| `org.graylog2.lookup.db.DBCacheService#findByIds`                                 | replaced by streamByIds                  |
| `org.graylog2.lookup.db.DBCacheService#findAll`                                   | replaced by streamAll                    |
| `org.graylog2.lookup.db.DBDataAdapterService#findByIds`                           | replaced by streamByIds                  |
| `org.graylog2.lookup.db.DBDataAdapterService#findAll`                             | replaced by streamAll                    |
| `org.graylog2.lookup.db.DBLookupTableService#findByCacheIds`                      | replaced by streamByCacheIds             |
| `org.graylog2.lookup.db.DBLookupTableService#findByDataAdapterIds`                | replaced by streamByDataAdapterIds       |
| `org.graylog2.lookup.db.DBLookupTableService#findAll`                             | replaced by streamAll                    |

DBService classes' new streaming methods require streams to be closed after using - recommend using try-with-resource statements.

## REST API Endpoint Changes

The following REST API changes have been made.

| Endpoint                                         | Description                                                                                                                     |
|--------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| `GET /tbd`                                       | tbd                                                                                                                             |
