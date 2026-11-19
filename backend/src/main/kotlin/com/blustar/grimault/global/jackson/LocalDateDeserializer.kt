package com.blustar.grimault.global.jackson

import org.springframework.boot.jackson.JacksonComponent
import tools.jackson.core.JsonParser
import tools.jackson.core.JsonToken
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer
import java.time.LocalDate
import java.time.format.DateTimeParseException

@JacksonComponent
class LocalDateDeserializer : ValueDeserializer<LocalDate>() {

    override fun deserialize(
        parser: JsonParser,
        context: DeserializationContext,
    ): LocalDate {
        if (parser.currentToken() != JsonToken.VALUE_STRING) {
            return context.reportInputMismatch(
                LocalDate::class.java,
                "Date must be a string in yyyy-MM-dd format",
            )
        }

        val value = parser.string

        if (!DATE_PATTERN.matches(value)) {
            return context.reportInputMismatch(
                LocalDate::class.java,
                "Date must be in yyyy-MM-dd format",
            )
        }

        return try {
            LocalDate.parse(value)
        } catch (_: DateTimeParseException) {
            context.reportInputMismatch(
                LocalDate::class.java,
                "Invalid date: $value",
            )
        }
    }

    companion object {
        private val DATE_PATTERN = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")
    }

}
