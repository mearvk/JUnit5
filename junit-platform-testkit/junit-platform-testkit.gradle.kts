plugins {
	`java-library-conventions`
	// id("org.assertj.generator") version "0.0.6b"
}

description = "JUnit Platform Test Kit"

/*
sourceSets {
	main {
		// must specify assertJ block to have it applied
		assertJ { }
	}
}
*/

dependencies {
	api("org.apiguardian:apiguardian-api:${Versions.apiGuardian}")
	api("org.assertj:assertj-core:${Versions.assertJ}")
	api("org.opentest4j:opentest4j:${Versions.ota4j}")

	api(project(":junit-platform-launcher"))
}
