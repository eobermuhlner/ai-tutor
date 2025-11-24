package ch.obermuhlner.aitutor.testharness

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication(
    exclude = [
        // Exclude auto-configurations that we don't need and might fail to initialize
        DataSourceAutoConfiguration::class  // We don't need a datasource for the testharness
    ],
    scanBasePackages = ["ch.obermuhlner.aitutor.testharness"]
)
class TestHarnessApplication

fun main(args: Array<String>) {
    runApplication<TestHarnessApplication>(*args)
}