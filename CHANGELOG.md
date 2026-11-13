<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# MongoDeclaration Changelog

## [Unreleased]

### Added
- Initial implementation of the `ReplaceDefWithExplicitType` inspection for Groovy code.
- Quick-fix action to replace dynamic `def` and `var` keywords with statically inferred explicit types.
- Preservation of existing modifiers (e.g., `final`) when transforming variable declarations.
- Support for multi-variable declarations with uniform types across variables.
- Automated test coverage validating inspections, quick-fix transformations, and edge cases.
