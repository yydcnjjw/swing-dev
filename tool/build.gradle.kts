plugins {
    java
    application    
    kotlin("jvm")
}

application {
    mainClassName = "org.yydcnjjw.swing.tool.XmlToolKt"
}

dependencies {
    implementation(project(":core"))
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.0-RC")
    
    // command line parse
    implementation("com.github.ajalt:clikt:2.1.0")

}
