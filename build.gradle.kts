import com.soywiz.korge.gradle.*

buildscript {
	repositories {
		mavenLocal()
		maven { url = uri("https://dl.bintray.com/korlibs/korlibs") }
		maven { url = uri("https://plugins.gradle.org/m2/") }
		mavenCentral()
		google()
	}
	dependencies {
		classpath("com.soywiz.korlibs.korge.plugins:korge-gradle-plugin:1.15.0.0")
		classpath("io.reactivex.rxjava3:rxkotlin:3.0.1")
	}
}

apply<KorgeGradlePlugin>()

korge {
	id = "com.yoloroy.yandex.cup.try.try"
}
