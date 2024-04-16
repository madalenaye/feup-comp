.class Test
.super java/lang/Object

.field intField I

.method public <init>()V
    aload_0
    invokespecial java/lang/Object/<init>()V
    return
.end method

.method public static main([Ljava/lang/String;)V
    return
.end method

.method public foo()I
    aload_0
    bipush 10
    putfield intField I
    aload_0
    getfield intField I
    istore_1
    iload_1
    ireturn
.end method
