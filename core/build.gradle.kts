plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("org.reflections:reflections:0.9.11")
    testImplementation(kotlin("test-junit"))
}