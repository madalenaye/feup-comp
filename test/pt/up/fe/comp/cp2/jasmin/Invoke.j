OllirToJasminInvoke.ollir:
.class Test
.super java/lang/Object
;default constructor
.method public <init>()V
   aload_0
   invokespecial java/lang/Object/<init>()V
   return
.end method
.method public static main([Ljava/lang/String;)V
   .limit stack 99
   .limit locals 99
   
   new Test
   dup
   astore_1
   aload_1
   
   invokespecial Test/<init>()V
   pop
   
   return
.end method