<!--
This file is part of JavaSMT,
an API wrapper for a collection of SMT solvers:
https://github.com/sosy-lab/java-smt

SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

# How to install a SMT solver library for Windows (64-bit) for developers of JavaSMT

## Note:

These steps are not required for using JavaSMT as a library,
but only for developing JavaSMT with native solver support on a Windows machine.

## Instruction:

There are two ways for providing SMT solver libraries for Windows,
which are either to create symlinks or to copy the library files.

### With symlinks to the downloaded library:

Symbolic links allow for an automatic update as soon as a dependency is changed via Ant/Ivy.
Windows does not allow user-space symlinks, but requires administrator rights to create them.
Thus, we cannot check them into the repository. Please execute the following as administrator:

For Z3:
- mklink libz3.dll ..\..\java\runtime-z3\libz3.dll
- mklink libz3java.dll ..\..\java\runtime-z3\libz3java.dll

For MathSAT5:
- mklink mpir.dll ..\..\java\runtime-mathsat\mpir.dll
- mklink mathsat.dll ..\..\java\runtime-mathsat\mathsat.dll
- mklink mathsat5j.dll ..\..\java\runtime-mathsat\mathsat5j.dll

### With a direct copy of the library:

An alternative simple solution (without the need of administrator) is to copy over
those files from the `lib\java\runtime-*\` directory into the current directory.
Please note that this needs to be repeated after each update of Ant/Ivy dependencies.

For Z3:
- copy ..\..\java\runtime-z3\libz3.dll libz3.dll
- copy ..\..\java\runtime-z3\libz3java.dll libz3java.dll

For MathSAT5:
- copy ..\..\java\runtime-mathsat\mpir.dll mpir.dll
- copy ..\..\java\runtime-mathsat\mathsat.dll mathsat.dll
- copy ..\..\java\runtime-mathsat\mathsat5j.dll mathsat5j.dll

Or simply use a wildcard:
- copy ..\..\java\runtime-*\*dll .\

## Additional dependencies:

To execute JavaSMT with MathSAT on a Windows system,
please install the [Visual C++ 2013 Redistributable Package](https://support.microsoft.com/en-us/help/4032938/update-for-visual-c-2013-redistributable-package).
Otherwise you might get an `UnsatisfiedLinkError` because of the missing system library `msvcr120.dll`.
