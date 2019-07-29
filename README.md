# Table of contents
1. [About liquibase-hive](#about-liquibase-hive)
1. [How to use](#how-to-use)
    - [with a Maven plugin](#with-a-maven-plugin)
    - [Liquibase-impala specific configuration](#liquibase-hive-specific-configuration)

# About liquibase-hive
Liquibase-impala is a [Liquibase](http://www.liquibase.org/) [extension](https://liquibase.jira.com/wiki/spaces/CONTRIB/overview), which adds support for Apache Hive.

# How to use

## with a Maven plugin
To use liquibase-impala in concert with `liquibase-maven-plugin`:
1. Make sure liquibase-impala is present in your local or remote (internal) Maven repo.
1. Add the following to your `pom.xml` file:
    ```xml
    <build>
      <plugins>
        <!-- (...) -->
        <plugin>
          <groupId>org.liquibase</groupId>
          <artifactId>liquibase-maven-plugin</artifactId>
          <version>${liquibase.version}</version>
          <dependencies>
            <!-- (...) -->
            <dependency>
              <groupId>org.liquibase.ext.hive</groupId>
              <artifactId>liquibase-hive</artifactId>
              <version>${liquibase.hive.version}</version>
            </dependency>
          </dependencies>
        </plugin>
      </plugins>
    </build>
    ```
1. Run Liquibase as you normally would using Maven plugin, for example:
    ```bash 
    mvn liquibase:update \
      -Dliquibase.changeLogFile=changelog/changelog.xml \
      -Dliquibase.driver=com.cloudera.hive.jdbc41.HS2Driver \
      -Dliquibase.username=<user>
      -Dliquibase.password=<password>
      -Dliquibase.url=jdbc:hive2://<host>:<port>/<database>;UID=<user>;UseNativeQuery=1
    ```

## Liquibase-hive specific configuration

Liquibase-hive provides additional configuration parameters that can be used to influence its behaviour:

| parameter         | values                | description                                       |
| ----------------- | --------------------- | ------------------------------------------------- |
| liquibase.lock    | true (default), false | enables/disables locking facility for a given job |

