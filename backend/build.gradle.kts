plugins {
	java
	id("org.springframework.boot") version "3.5.3"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.vims"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	
	// 이메일 전송
	implementation("org.springframework.boot:spring-boot-starter-mail")

	// WebRTC 시그널링용 JSON 처리
	implementation("com.fasterxml.jackson.core:jackson-databind")

	// ✅ Gson (Kurento 의존성 해결)
	implementation("com.google.code.gson:gson:2.8.9")

	// WebSocket STOMP 지원
	implementation("org.springframework:spring-messaging")

	// Kurento Media Server 클라이언트
	implementation("org.kurento:kurento-client:6.18.0")
	implementation("org.kurento:kurento-jsonrpc-client:6.18.0")

	// DB
	runtimeOnly("com.h2database:h2")
	implementation("mysql:mysql-connector-java:8.0.33")

	// JWT 토큰 처리
	implementation("io.jsonwebtoken:jjwt-api:0.12.3")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

	// Lombok
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	// Test
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
