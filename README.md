# Tongs
A kitchen tongs for distributing your candies across all the tasters/testers. It saves your time by running test cases in parallel on multiple devices simultaneously.

Tongs is based on [Fork](https://github.com/shazam/fork). It distributes execution of test cases across multiple devices making test runs much faster. And it provides valuable reports for them.

## Differences from similar tools
* Full support for JUnit parameterized test cases - test case variants are distributed across devices
* Custom instrumentation runner arguments are supported, including filters
* Gradle support, including: 
  * Support for split APKs
  * Install APKs with Gradle
* Tongs clears application data before test case execution
* Support for test frameworks other than JUnit 4 assuming their runners are compatible with AndroidJUnitRunner (see [Custom runners])

## Running Tongs

### Gradle plugin (recommended)
1. Add JitPack repository (this service builds packages directly from GitHub sources) and classpath plugin dependencies
```
buildscript {
  repositories {
    // other repos ...
    maven { url 'https://jitpack.io' }
  }
  dependencies {
    classpath 'com.github.TarCV:tongs:tongs-gradle-plugin:master-SNAPSHOT'
  }
}
```
2. Apply the plugin
```
apply plugin: 'com.github.tarcv.tongs'
``` 
3. Check tasks added by the plugin
```
./gradlew tasks | grep tongs
```
4. Run one of those tasks. For example:
```
./gradle tongs
```

## Future plans
* Full support for JUnit assumptions
* Plugins
* Device flakiness reports

More docs soon

For now check [repository of the project](https://github.com/shazam/fork) Tongs is based on.
