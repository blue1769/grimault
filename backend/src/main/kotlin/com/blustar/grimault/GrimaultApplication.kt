package com.blustar.grimault

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GrimaultApplication

fun main(args: Array<String>) {
	runApplication<GrimaultApplication>(*args)
}
