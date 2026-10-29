package com.blustar.grimault.domain.category.validation

import com.blustar.grimault.domain.category.dto.CreateCategoryRequest
import com.blustar.grimault.domain.category.entity.CategoryType
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class CreateCategoryRequestValidator :
    ConstraintValidator<ValidCreateCategoryRequest, CreateCategoryRequest> {

    override fun isValid(
        body: CreateCategoryRequest?,
        context: ConstraintValidatorContext,
    ): Boolean {
        if (body == null) {
            return true
        }

        if (body.name.isBlank()) {
            return invalid(context, "Category name must not be blank")
        }

        CategoryType.fromNameOrNull(body.type.trim())
            ?: return invalid(context, "Invalid category type")

        return true
    }

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