# EZ-Cfg [Minecraft]
A Minecraft dev-Library for making work with configuration files easier and faster
## Usage
The following example gives an example of usage (uses Lombok)
```java
@Data
public class MySetting implements YamlConfigData<MySetting, MyPlugin> {
    // Required
    @NonNull @Setter(AccessLevel.NONE) private final MyPlugin plugin;
    // Required
    private YamlConfiguration yamlFile;

    @CfgField private String myString = "foo";
    @CfgField("IsSomeOptionEnabled") private boolean optionEnabled = true;

    // Complex objects
    @CfgField("holograms.Vector") private Vector hologramsVector = new Vector(0.0, 1.25, 0.0);
    @CfgField("holograms.IDs") private List<Integer> hologramsIds = Collections.emptyList();
}
```
Then load the values using
```Java
MySetting setting = new MySetting().load();
MySetting setting = new MySetting().load(new File("path/to/File.yml));
MySetting setting = new MySetting().load("path/to/File.yml);
```
## Maven:
Due to this library using custom repositories it cannot be deployed to Maven Central so in order to use it you can use JitPack's repo:
```xml
<repositories>
  ...
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
  ...
</repositories>
```
```xml
<dependencies>
  ...
  <dependency>
    <groupId>com.github.JarvisCraft</groupId>
    <artifactId>ez-cfg-spigot<!-- or ez-cfg-bungee --></artifactId>
    <version>-SNAPSHOT</version>
  </dependency>
  ...
<dependencies>
```
