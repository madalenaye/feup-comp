.class Test
.super java/lang/Object
;default constructor
.field intField I
.method public <init>()V
   aload_0
   invokespecial java/lang/Object/<init>()V
   return
.end method
.method public static main([Ljava/lang/String;)V
   .limit stack 99
   .limit locals 99

   return
.end method
.method public foo()V
   .limit stack 99
   .limit locals 99
   aload_0
   bipush 10
   putfield Test/intField I
   aload_0
   getfield Test/intField I
   istore_1

   return
.end method