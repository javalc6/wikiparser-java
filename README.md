# Introduction
The class ``TemplateParser`` implements light wiki template parser, main logic is included in less than three hundred lines.
The Java Wiktionary parser converts Wiktionary wikicode to HTML using ``TemplateParser`` and ``WikiFormatter``.
``TemplateParser`` performs wikicode parsing, template expansion, parser functions execution and module processing.
``WikiFormatter`` formats the expanded wikitext to HTML.
Main software is written in Java and uses LUA modules from Wikimedia.

The library has been developed to parse and render English Wiktionary, starting from the dump **enwiktionary-latest-pages-articles.xml.bz2** available in https://dumps.wikimedia.org/enwiktionary/latest/

In addition to English, several other languages are supported, as described below in section ``Localization``.

As an example of a wiktionary definition parsed by ``TemplateParser`` and rendered by ``WikiFormatter``, please look at [time](wiki.html) definition.

# WikiSplitter
The ``WikiSplitter`` tool generates files wiki.dat, templates.dat and modules.dat from a wikimedia dump.
```
compile: javac -encoding UTF-8 wiki\WikiSplitter.java
usage:  java wiki.WikiSplitter <filename>
```
After the execution of ``WikiSplitter``, file **wiki.dat** will contain all the definitions in a plain text file, **templates.dat** will contain all templates in a plain text file and **modules.dat** will contain all modules in a plain text file. These files are needed for further processing.
# Usage example

Using the wiki parser is quite simple, once you have generated \*.dat files with ``WikiSplitter``, your own code can parse wikitext, expand it and render to html in two steps:
* parse and expand wikitext with ``tp.parse()``
* format expanded text to html with ``WikiFormatter.formatWikiText()``

Before doing these steps, you shall instantiate the helper ``WikiPage`` to guide the parser in the wiki expansion with information like date, locale, templates, modules.

Here is the typical code to perform wikitext parsing/expansion and rendering to html:
```java
TemplateParser tp = new TemplateParser();
WikiPage wp = new WikiPage(keyword,  date, Locale.ENGLISH, tp, name2template, name2module, false, name2content, true);
String expanded = tp.parse(definition, wp);
String formatted = WikiFormatter.formatWikiText(new StringBuilder(keyword), new StringBuilder(expanded), linkBaseURL);
```
The string ``formatted`` will contain the html related to the given definition. 
In the ``wiki`` folder you can find the WikiFind class that can be used as starting point to write your own application class.
# WikiFind
Class ``WikiFind`` is demo that searchs and renders a wiki page into html format using files wiki.dat, templates.dat and modules.dat generated by WikiSplitter.
```
compile: javac -encoding UTF-8 -cp .;lib\luaj-jse-3.0.2p.jar wiki\WikiFi
nd.java
usage:  java -cp .;lib\luaj-jse-3.0.2p.jar wiki.WikiFind -random|-longest|<word>

meaning of options:
-random: search a random word
-longest: search word with longest definition
```
# TestSuite
Class ``TestSuite`` performs automatic tests of the wiki parser.
```
compile: javac -encoding UTF-8 -cp .;lib\luaj-jse-3.0.2p.jar wiki\TestSuite.java
usage: java -cp .;lib\luaj-jse-3.0.2p.jar wiki.TestSuite [test number from 0 to 4] [template to test]

meaning of test number:
0: miscellaneous tests to check features
1: debug test
2: test template expansion, in this case specify also the template to test in the command line, note: requires files templates.dat and modules.dat
3: smoke test, note: requires files wiki.dat, templates.dat and modules.dat
4: parser robustness test

template to test: this parameter is required only for test number 2

```
# Classes
Main classes:
- wiki.TestSuite, a test suite for ``TemplateParser``
- wiki.WikiSplitter, a tool to generate files wiki.dat, templates.dat and modules.dat from a wikimedia dump
- wiki.WikiFind, demo that searchs and renders a wiki page into html format using files wiki.dat, templates.dat and modules.dat generated by ``WikiSplitter``

Wiki parser classes:
- wiki.TemplateParser, parser of wiki templates
- wiki.MagicWords, implementation of wiki magic words
- wiki.NameSpaces, wikipedia namespaces
- wiki.tools.Utilities, miscellaneous utility functions
- wiki.tools.WikiFormatter, html renderer for wikicode not related to templates and modules
- wiki.tools.WikiPage, helper for template expansion
- wiki.tools.WikiScanner, scanner for wiki text
- wiki.parserfunctions.\*, classes that implement wiki parser functions
- wiki.parserfunctions.ParserFunction, abstract class for parser functions
- wiki.parserfunctions.ParserFunctions, dictionary of parser functions

Support libraries:
- info.bliki.extensions.scribunto.\*, classes that implements the interface to luaj, based on https://github.com/axkr/info.bliki.wikipedia_parser 
- info.bliki.wiki.template.dates.\*, classes that emulate the strtotime() php function
- bzip2.\*, classes to unpack bzip2 files

- luaj, java interpreter of Lua language, used to process wiki modules, reference: https://github.com/luaj/luaj

- luabit\\\*.lua, lualib\\\*.lua and ustring\\\*.lua: lua scripts, reference: https://github.com/wikimedia/mediawiki-extensions-Scribunto/tree/master/includes/Engines/LuaCommon/lualib

# Caveats
This wiki parser is simplified and light, but has several limitations.
It does not aim to achieve 100% features of php based wiki parser and it is designed for the wiktionary.
The parser is not optimized for speed.

Warning: the constant ``strict_Lua_invocation`` in class WikiPage is set to ``false`` for speed, but in order to get accurate results the value shall be set to ``true``.

# Localization
Language localization is supported by defining properties file in the ``wiki`` folder with filename wiktionary_\<language code\>.properties

The following optional properties may be defined in localization files:
- ``thislanguage`` and ``language_pattern``: filter criteria to extract only wanted language
- ``module``: localized label for Module
- ``template``: localized label for Template
- ``redirect``: localized label for Redirect
- ``parser.if``: alias for #if
- ``parser.iferror``: alias for #iferror
- ``parser.ifexist``: alias for #ifexist
- ``parser.ifexpr``: alias for #ifexpr
- ``parser.ifeq``: alias for #ifeq

Following languages are supported in the current implementation:
- ``Arab``
- ``Dutch``
- ``English`` (default)
- ``French``
- ``German``
- ``Greek``
- ``Italian``
- ``Latin``
- ``Portuguese``
- ``Romanian``
- ``Russian``
- ``Spanish``
- ``Turkish``

The magicword ``int`` requires additional language files that shall be downloaded from https://gerrit.wikimedia.org/g/mediawiki/core/%2B/HEAD/languages/i18n and stored in folder ``wiki``.

# Credits
This software integrates 3pp software to perform specific activities.
The following 3pp are used by this software:
- java lua interpreter luaj: https://github.com/luaj/luaj
- wikimedia lua scripts: https://github.com/wikimedia/mediawiki-extensions-Scribunto/tree/master/includes/Engines/LuaCommon
- java scribunto engine: https://github.com/axkr/info.bliki.wikipedia_parser/tree/master/bliki-core/src/main/java/info/bliki/extensions/scribunto
- java strtotime function: https://github.com/axkr/info.bliki.wikipedia_parser/tree/master/bliki-core/src/main/java/info/bliki/wiki/template/dates/
- bzip2: https://commons.apache.org/proper/commons-compress
