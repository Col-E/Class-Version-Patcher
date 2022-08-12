# Class Version Patcher

This plugin allows you to utilize all the new language features of Java while targeting an older version's bytecode. 

## Usage - Maven

To integrate into your project:

1. Install the plugin locally
2. Add the plugin to your project
    - Update the scope of any dependency that is future-versioned to `<scope>provided</scope>`
3. Run `compile` on your project to generate the modified classes in the `%PROJECR%/target/classes` directory
4. Run any phase you like, such as `test` _(They should now see the modified classes)_


### Installing the plugin locally

```
git clone https://github.com/Col-E/Class-Version-Patcher.git
cd Maven-Class-Patcher
mvn install
```

### Using the plugin in your `pom.xml`
```xml
<build>
    <plugins>
        <plugin>
            <groupId>software.coley</groupId>
            <artifactId>class-version-patcher-maven</artifactId>
            <version>2.1.0</version>
            <configuration>
                <!-- Target Java 8, can swap for other version such as 11 -->
                <targetVersion>8</targetVersion>
                <artifacts>
                    <!-- The parameters are the group + artifact identifiers of any dependency separated by a colon ":"
                         You can include any number of values here.
                     -->
                    <param>group_id:artifact_id</param>
                    <param>org.jgroups:jgroups</param>
                </artifacts>
            </configuration>
            <!-- Required to run -->
            <executions>
                <execution>
                    <goals>
                        <goal>patch-compiled</goal>
                        <goal>patch-dependencies</goal>
                        <goal>patch-postprocess</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

## Usage - Gradle

> NOTE: The gradle plugin is not yet developed, however you can add the `core` module of this project as a dependency to your buildscript.
> From there, you can invoke `software.coley.versionpatcher.VersionPatcher` as you see fit.

## This doesn't properly downgrade Java X's "xyz-feature" to Java Y's version!

Please open an issue with an example of how to replicate your problem.

For example:

- Providing a class file
- Listing a [maven dependency](https://mvnrepository.com/artifact/org.jgroups/jgroups/5.1.2.Final)
- Listing an [open source project](https://github.com/belaban/JGroups)