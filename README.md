[ ![Download](https://api.bintray.com/packages/act262/maven/embed-artifacts/images/download.svg) ](https://bintray.com/act262/maven/embed-artifacts/_latestVersion)

### What

Merge multiple jar/aar file into single jar/aar, so no maven transitive.


### Features
- [x] Support merge multiple aar/jar into one
- [x] Support merge aar's  AndroidManifest with placeholders
- [x] Support merge aar's proguard.txt
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
        classpath 'io.zcx.plugin:embed-artifacts:<latest-version>'
    }
}

```

library's `build.gradle`

```groovy
apply plugin: 'com.android.library'

// apply embed plugin
apply plugin: 'io.zcx.plugin.embed-artifacts'
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