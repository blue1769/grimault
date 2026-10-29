package com.blustar.grimault.domain.category.validation

import jakarta.validation.Constraint
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Constraint(validatedBy = [CreateCategoryItemRequestValidator::class])
annotation class ValidCreateCategoryItemRequest(
    val message: String = "Invalid category item request",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Any>> = [],
)
