package com.example.urnodeswidget.data

import com.google.gson.annotations.SerializedName

data class TransferStatsResponse(
    @SerializedName("unpaid_bytes_provided")
    val unpaidBytesProvided: Long,
    @SerializedName("paid_bytes_provided")
    val paidBytesProvided: Long // Added for completeness
)