package com.blustar.grimault.domain.category.validation

import jakarta.validation.Constraint
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Constraint(validatedBy = [CreateCategoryRequestValidator::class])
annotation class ValidCreateCategoryRequest(
    val message: String = "Invalid category request",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Any>> = [],
)
