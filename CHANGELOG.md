# Change Log for Tapis Applications Service

All notable changes to this project will be documented in this file.

Please find documentation here:
https://tapis.readthedocs.io/en/latest/technical/apps.html

You may also reference live-docs based on the openapi specification here:
https://tapis-project.github.io/live-docs

## 1.1.0 - 2022-01-07

New minor release.

### New features:
- None

### Bug fixes:
- None

## 1.0.2 - 2021-12-16

Incremental improvements and bug fixes.

### New features:
- Add isMpi, mpiCmd and cmdPrefix attributes to jobAttributes.

### Bug fixes:
- Allow nulls for additional attributes in order to support GET + PUT.

## 1.0.1 - 2021-11-19

Incremental improvements and bug fixes.

### New features:
- Support multiple orderBy
- Re-design FileInput, ParameterSet arguments (app arguments, etc).
- Add FileInputArray
- Rename appType to jobType and make it an optional versioned attribute.

### Bug fixes:
- Fix problems with setting of defaults for FileInputs and ParameterSet arguments.

## 1.0.0 - 2021-07-16

Initial release supporting basic CRUD operations on Tapis Application resources.

### Breaking Changes:
- Initial release.

### New features:
 - Initial release.

### Bug fixes:
- None.
