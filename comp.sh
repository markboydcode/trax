
rm -rf classes/nbdp

javac -sourcepath ./src/main/java -d ./classes  -cp jars/apache-log4j-1.2.17/log4j-1.2.17.jar:jars/commons-logging-1.1.2/commons-logging-1.1.2.jar:jars/spring-framework-3.2.2.RELEASE/libs/spring-beans-3.2.2.RELEASE.jar:jars/spring-framework-3.2.2.RELEASE/libs/spring-core-3.2.2.RELEASE.jar:jars/commons-dbcp-1.4/commons-dbcp-1.4.jar:jars/mybatis-3.2.1/mybatis-3.2.1.jar:jars/mybatis-spring-1.2.0/mybatis-spring-1.2.0.jar:jars/spring-framework-3.2.2.RELEASE/libs/spring-orm-3.2.2.RELEASE.jar ./src/main/java/nbdp/trax/*.java

javac -sourcepath ./src/main/java -d ./classes  -cp jars/apache-log4j-1.2.17/log4j-1.2.17.jar:jars/commons-logging-1.1.2/commons-logging-1.1.2.jar:./classes:jars/mybatis-3.2.1/mybatis-3.2.1.jar:jars/mybatis-spring-1.2.0/mybatis-spring-1.2.0.jar:jars:jars/spring-framework-3.2.2.RELEASE/libs/spring-jdbc-3.2.2.RELEASE.jar:jars/spring-framework-3.2.2.RELEASE/libs/spring-orm-3.2.2.RELEASE.jar:jars/spring-framework-3.2.2.RELEASE/libs/spring-tx-3.2.2.RELEASE.jar:jars/spring-framework-3.2.2.RELEASE/libs/spring-beans-3.2.2.RELEASE.jar:jars/spring-framework-3.2.2.RELEASE/libs/spring-core-3.2.2.RELEASE.jar ./src/main/java/nbdp/trax/data/ibatis/*.java



