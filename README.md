[![Maven](https://img.shields.io/maven-metadata/v/http/jcenter.bintray.com/com/jfz/plugin/embed-artifact/maven-metadata.xml.svg)](http://jcenter.bintray.com/com/jfz/plugin/embed-artifact/)

### What

Merge multiple jar/aar file into single jar/aar, so no maven transitive.


### Features
- [x] Support merge multiple aar/jar into one
- [x] Support merge aar's  AndroidManifest with placeholders
- [ ] Support merge aar's proguard.txt
- [ ] Support merge aar's public.txt
- [ ] Support merge all class with proguard


### Usage

> test on `Android gradle plugin 3.1.+`, `Gradle 4.4`,`AndroidStudio 3.+`

root's `build.gradle`
```groovy
buildscript {
    repositories {
        // for local test 
        mavenLocal()
        
        google()
        jcenter()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:3.1.4'
      
        // Add here
        classpath 'com.jfz.plugin:embed-artifact:<latest-version>'
    }
}

```

library's `build.gradle`

```groovy
apply plugin: 'com.android.library'

// apply embed plugin
apply plugin: 'com.jfz.plugin.embed-artifact'
```

and dependencies' replace `api`,`implementation` with `embed`  what you want to merge 
```groovy
dependencies {
  
    // 微博SDK aar
//    api 'com.sina.weibo.sdk:core:4.2.7:openDefaultRelease@aar'

    embed 'com.sina.weibo.sdk:core:4.2.7:openDefaultRelease@aar'

    // 微信SDK jar
//    api 'com.tencent.mm.opensdk:wechat-sdk-android-without-mta:5.1.4'

    embed 'com.tencent.mm.opensdk:wechat-sdk-android-without-mta:5.1.4'
}
```