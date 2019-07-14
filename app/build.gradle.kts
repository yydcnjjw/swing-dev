plugins {
    java
    application
    kotlin("jvm")
}

application {
    mainClassName = "org.yydcnjjw.application.Main"
}

dependencies {
    implementation(project(":core"))
    implementation(kotlin("stdlib"))
}
