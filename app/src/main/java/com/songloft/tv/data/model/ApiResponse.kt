package com.songloft.tv.data.model

data class ApiResponse<T>(
    val data: T?,
    val error: String?,
    val detail: String? = null
) {
    val isSuccess: Boolean get() = error == null && data != null
}

data class PaginatedResponse<T>(
    val items: List<T>,
    val total: Int,
    val limit: Int = 50,
    val offset: Int = 0
)
