The code under notepad package is a heavily reworked version of richedit control from ICU project (http://www.icu-project.org/).
The most effort went into making the edit control take advantage of fractional metrics and removing unnecessary dependencies.
Unlike the rest of org.jezve.* code, this package may happen to use generics and most likely is NOT JDK 1.4 source-compatible.

There're many things that are broken or missing from this code:
 - text serialisation
 - clipboard
 - editing actions / menus / keyboard shortcuts
 - (named) text styles
to name a few.

Use at your own risk.
