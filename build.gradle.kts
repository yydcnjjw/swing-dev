plugins {
    base
    kotlin("jvm") version "1.3.41" apply false
}

allprojects {
    group = "org.yydcnjjw.swing"
    version = "1.0"

    repositories {
        maven(url="https://maven.aliyun.com/repository/public/")
        jcenter()
        mavenCentral()
    }
}

dependencies {
    subprojects.forEach {
        archives(it)
    }
}