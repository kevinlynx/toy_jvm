@echo off
set PATH=D:\Program Files\Java\jdk1.8.0_74\bin;%PATH%
java -Djava.util.logging.config.file=logging.properties -jar toy_jvm.jar -cp .;runtime %*
