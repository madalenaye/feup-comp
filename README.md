# Comp2024 Project

Contains a reference implementation for an initial version of the project that supports a small portion of Java--.

| test                                               | file                |
| -------------------------------------------------- | ------------------- |
| undeclared variable                                | DeclarationVerifier |
| class not imported                                 | DeclarationVerifier |
| undeclared method call                             | DeclarationVerifier |
| method call in extends                             | DeclarationVerifier |
| method call in imports                             | DeclarationVerifier |
| int + object                                       | TypeVerifier        |
| bool \* int                                        | TypeVerifier        |
| array + int                                        | TypeVerifier        |
| int = bool                                         | TypeVerifier        |
| object assign fail                                 | TypeVerifier        |
| object assign pass extends                         | TypeVerifier        |
| object assign pass imports                         | TypeVerifier        |
| neg expression                                     | TypeVerifier        |
| if (123)                                           | TypeVerifier        |
| while([1,2,3])                                     | TypeVerifier        |
| incompatible arguments                             | TypeVerifier        |
| incompatible return                                | TypeVerifier        |
| in method expr first expr must be varRefrExp ?     | TypeVerifier        |
| assume arguments                                   | TypeVerifier        |
| array access on int                                | ArrayVerifier       |
| array index not int                                | ArrayVerifier       |
| array Init                                         | ArrayVerifier       |
| this cannot be in static method                    | MethodVerifier      |
| only main is static                                | MethodVerifier      |
| superclass needs to be imported                    | MethodVerifier      |
| varargs                                            | VarargsVerifier     |
| variable, fields, method returns cannot be varargs | VarargsVerifier     |

## DeclarationVerifier

1. undeclared variables - VAR_REF_EXPR ✔
2. class not imported - METHOD_EXPR ✔
3. undeclared method call - METHOD_EXPR ✔
4. method call in extends - METHOD_EXPR ✔
5. method call in imports - METHOD_EXPR ✔

## TypeVerifier

6. int + object - BINARY_EXPR ✔
7. bool \* int - BINARY_EXPR ✔
8. array + int - BINARY_EXPR ✔
9. int = bool - ASSIGN_STMT ✔
10. object assign fail - ASSIGN_STMT ✔
11. object assign pass extends - ASSIGN_STMT ✔
12. object assign pass imports - ASSIGN_STMT ✔
13. if (123) - IF_STMT ✔
14. while([1,2,3]) - WHILE_STMT ✔
15. neg expr - NEG_EXPR ✔
16. incompatible arguments ✔
17. incompatible return ✔

## ArrayVerifier

18. array access on int - Array ✔
19. array index not int ✔
20. array init ✔

## VarArgsVerifier

21. variable, fields, method returns cannot be varargs ✔