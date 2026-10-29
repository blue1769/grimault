package com.blustar.grimault.domain.category.validation

import com.blustar.grimault.domain.category.dto.CreateCategoryItemRequest
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class CreateCategoryItemRequestValidator :
    ConstraintValidator<ValidCreateCategoryItemRequest, CreateCategoryItemRequest> {

    override fun isValid(
        body: CreateCategoryItemRequest?,
        context: ConstraintValidatorContext,
    ): Boolean {
        if (body == null) {
            return true
        }

        if (body.name.isBlank()) {
            return invalid(context, "Category item name must not be blank")
        }

        return true
    }

    @Suppress("SameParameterValue")
    private fun invalid(
        context: ConstraintValidatorContext,
        message: String,
    ): Boolean {
        context.disableDefaultConstraintViolation()
        context
            .buildConstraintViolationWithTemplate(message)
            .addConstraintViolation()

        return false
    }

}