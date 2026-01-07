luaj-jse-3.0.1.jar: this version is compatible with wikipedia (LUA 5.1)
luaj-jse-3.0.2.jar: this version is not fully compatible with wikipedia (e.g. it may cause error invalid use of '%' in replacement string: after '%' must be '0'-'9' or '%')
luaj-jse-3.0.2p.jar: StringLib.java patched to be compatible with wikipedia (without error invalid use of '%' in replacement string: after '%' must be '0'-'9' or '%') + patch for os.date with format string "!*t" in OsLib.java
luaj-jse-3.0.2q.jar: changed value of LUAI_MAXVARS from 200 to 240 and corrected TableLib.concat() to handle nil value for parameter sep
