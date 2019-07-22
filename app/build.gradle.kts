plugins {
    java
    application
    kotlin("jvm")
}

application {
    mainClassName = "org.yydcnjjw.application.MainKt"
}

dependencies {
    implementation(project(":core"))
    implementation(kotlin("stdlib"))
}
