package org.example

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExampleTest {
    @Test
    fun `gets the correct wiremock version from the running container`() {
        val version = Example().getWiremockVersion()
        assertTrue(version.contains("3.9.1"))
    }
}
