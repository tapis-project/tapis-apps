# Change Log for Tapis Applications Service

All notable changes to this project will be documented in this file.

Please find documentation here:
https://tapis.readthedocs.io/en/latest/technical/apps.html

You may also reference live-docs based on the openapi specification here:
https://tapis-project.github.io/live-docs

---------------------------------------------------------------------------
## 1.2.4 - 2022-11-02

Incremental improvements and new preview features.

### New features:
- Add support for searching by *tags* attribute using operator *contains*.
- Add support for query parameter *listType* when retrieving systems. Allows for filtering based on authorization.
    * Options are OWNED, SHARED_PUBLIC, ALL. Default is OWNED.
- Improved error message when attempting to search using an unsupported attribute

---------------------------------------------------------------------------
## 1.2.3 - 2022-10-15

Incremental improvements and new preview features.

### New features:
- Add attribute *notes* to *ArgSpec*
- Add attribute *description* to *envVariables* in *ParameterSet*.

### Bug fixes:
- None

---------------------------------------------------------------------------
## 1.2.2 - 2022-10-10

Incremental improvements and bug fixes.

### Bug fixes:
- Fix issues with handling of authorization checks for service requests.

---------------------------------------------------------------------------
## 1.2.1 - 2022-09-01

Incremental improvements and new features.

### New features:
- New endpoints for application sharing. getApp returns sharedAppCtx attribute.

### Bug fixes:
- None

---------------------------------------------------------------------------
## 1.2.0 - 2022-05-23

Incremental improvements and new features.

### New features:
- Support impersonationId for service to service requests.

### Bug fixes:
- None

---------------------------------------------------------------------------
## 1.1.3 - 2022-05-07

Incremental improvements and preview of new features.

### New features:
- Refactor authorization checks for maintainability.
- Replace skipTapisAuthorization with impersonationId for requests from Jobs service.

### Bug fixes:
- None

---------------------------------------------------------------------------
## 1.1.2 - 2022-04-13

Preview of new features.

### New features:
- Additional information for Apps history.
- Support skipTapisAuthorization for requests from Jobs service.

### Bug fixes:
- None

---------------------------------------------------------------------------
## 1.1.1 - 2022-03-03

Incremental improvements and bug fixes.

### New features:
- Update readyCheck to check for expired service JWT.
- Updates for JDK 17

### Bug fixes:
- None

---------------------------------------------------------------------------
## 1.1.0 - 2022-01-07

New minor release.

### New features:
- None

### Bug fixes:
- None

---------------------------------------------------------------------------
## 1.0.2 - 2021-12-16

Incremental improvements and bug fixes.

### New features:
- Add isMpi, mpiCmd and cmdPrefix attributes to jobAttributes.

### Bug fixes:
- Allow nulls for additional attributes in order to support GET + PUT.

---------------------------------------------------------------------------
## 1.0.1 - 2021-11-19

Incremental improvements and bug fixes.

### New features:
- Support multiple orderBy
- Re-design FileInput, ParameterSet arguments (app arguments, etc).
- Add FileInputArray
- Rename appType to jobType and make it an optional versioned attribute.

### Bug fixes:
- Fix problems with setting of defaults for FileInputs and ParameterSet arguments.

---------------------------------------------------------------------------
## 1.0.0 - 2021-07-16

Initial release supporting basic CRUD operations on Tapis Application resources.

### Breaking Changes:
- Initial release.

### New features:
 - Initial release.

### Bug fixes:
- None.
