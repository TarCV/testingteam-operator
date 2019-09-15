package com.github.tarcv.tongs.model

import com.google.gson.JsonObject
import java.util.*
import java.util.Collections.emptyMap

data class TestCase @JvmOverloads constructor(
        val testMethod: String,
        val testClass: String,
        val properties: Map<String, String> = emptyMap()
)
