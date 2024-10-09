package org.example

import ApiClient

class Example {
    fun getWiremockVersion(): String {
        return ApiClient().request("http://localhost:8080/__admin/version")
    }
}
