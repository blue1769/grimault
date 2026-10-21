package com.blustar.grimault.domain.account.dto

import com.blustar.grimault.domain.account.validation.ValidUpdateAccountRequest
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

@Schema(description = "금융 계정 변경 요청")
@ValidUpdateAccountRequest
data class UpdateAccountRequest(

    // FIXME: reject blank-only account names in create requests as well
    @field:Size(max = 50)
    val name: String? = null,

    @field:Min(1)
    @field:Max(31)
    val settlementDay: Int? = null,

    @field:Min(-2)
    @field:Max(0)
    val startMonthOffset: Int? = null,

    @field:Min(1)
    @field:Max(31)
    val startDay: Int? = null,

    @field:Min(-2)
    @field:Max(0)
    val endMonthOffset: Int? = null,

    @field:Min(1)
    @field:Max(31)
    val endDay: Int? = null,
)
