# Introduction
The class TemplateParser implements light wiki template parser, main logic is included in less than three hundred lines.
The Java Wiktionary parser converts Wiktionary wikicode to HTML using TemplateParser and WikiFormatter.
TemplateParser performs wikicode parsing, template expansion, parser functions execution and module processing.
WikiFormatter formats the expanded wikitext to HTML.
Main software is written in Java and uses LUA modules from Wikimedia.

The library has been developed to parse and render English Wiktionary, starting from the dump enwiktionary-latest-pages-articles.xml.bz2 available in https://dumps.wikimedia.org/enwiktionary/latest/

As an example of a wiktionary definition parsed by TemplateParser and rendered by WikiFormatter, please look at [time](wiki.html) definition.

# WikiSplitter
The WikiSplitter tool generates files wiki.dat, templates.dat and modules.dat from a wikimedia dump.
```
compile: javac -encoding UTF-8 wiki\WikiSplitter.java
usage:  java wiki.WikiSplitter <filename>
```
# WikiFind
Class WikiFind is demo that searchs and renders a wiki page into html format using files wiki.dat, templates.dat and modules.dat generated by WikiSplitter.
```
compile: javac -cp .;lib\luaj-jse-3.0.2p.jar wiki\WikiFind.java
usage:  java -cp .;lib\luaj-jse-3.0.2p.jar wiki.WikiFind <word>
```
# TestSuite
Class TestSuite performs automatic tests of the wiki parser.
```
compile: javac -encoding UTF-8 -cp .;lib\luaj-jse-3.0.2p.jar wiki\TestSuite.java
usage: java -cp .;lib\luaj-jse-3.0.2p.jar wiki.TestSuite [test number from 0 to 4]
```
# Classes
Main classes:
- wiki.TestSuite, a test suite for TemplateParser
- wiki.WikiSplitter, a tool to generate files wiki.dat, templates.dat and modules.dat from a wikimedia dump
- wiki.WikiFind, demo that searchs and renders a wiki page into html format using files wiki.dat, templates.dat and modules.dat generated by WikiSplitter

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

- luaj, java interpreter of Lua language, used to process wiki modules, based on https://github.com/luaj/luaj

- luabit\\\*.lua, lualib\\\*.lua and ustring\\\*.lua: lua scripts, reference: https://github.com/wikimedia/mediawiki-extensions-Scribunto/tree/master/includes/Engines/LuaCommon/lualib

# Caveats
This wiki parser is simplified and light, but has several limitations. It is designed only for english wiktionary.
It does not aim to achieve 100% features of php based wiki parser and it is not tested versus wikipedia.
The parser is not optimized for speed.

# Todo
The following future improvements will enhance the parser:
- implement getEntityTable() in MwText.java to avoid hack in mw.text.lua (function mwtext.decode)
- language localization
- use also aliases defined in NameSpaces, to identify templates and modules
- interwiki links
- implement stubs with real code
- parser functions localurle and fullurle have been implemented as localurl and fullurl, instead they should use htmlspecialchars before returning the result
- implement missing tags, e.g. canonicalurl,  #categorytree and others
- define missing namespaces in class NameSpaces
- implement remaining magic words, e.g. pagesincat, pagesincategory, pagesinnamespace,....
- improve quality of html renderer
- improve performance

# Credits
This software integrates 3pp software to perform specific activities.
The following 3pp are used by this software:
- java lua interpreter luaj: https://github.com/luaj/luaj
- wikimedia lua scripts: https://github.com/wikimedia/mediawiki-extensions-Scribunto/tree/master/includes/Engines/LuaCommon
- java scribunto engine: https://github.com/axkr/info.bliki.wikipedia_parser/tree/master/bliki-core/src/main/java/info/bliki/extensions/scribunto
- java strtotime function: https://github.com/axkr/info.bliki.wikipedia_parser/tree/master/bliki-core/src/main/java/info/bliki/wiki/template/dates/
