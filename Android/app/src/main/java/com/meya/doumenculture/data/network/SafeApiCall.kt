package com.meya.doumenculture.data.network

import com.google.gson.Gson
import com.meya.doumenculture.data.model.ApiResponse
import retrofit2.HttpException
import java.io.IOException

private val gson = Gson()

private data class FastApiErrorDetail(
    val detail: String?
)

private data class WrappedErrorMessage(
    val message: String?
)

suspend inline fun <T> safeApiCall(crossinline call: suspend () -> ApiResponse<T>): ApiResult<T> {
    return try {
        val response = call()
        if (response.code == 0) {
            @Suppress("UNCHECKED_CAST")
            ApiResult.Success(response.data as T)
        } else {
            ApiResult.Error(response.code, response.message)
        }
    } catch (e: HttpException) {
        val body = e.response()?.errorBody()?.string()
        val message = parseErrorMessage(body) ?: e.message() ?: "请求失败"
        ApiResult.Error(e.code(), message)
    } catch (_: IOException) {
        ApiResult.Error(-1, "网络连接失败，请检查网络")
    } catch (e: Exception) {
        ApiResult.Error(-1, e.message ?: "未知错误")
    }
}

fun parseErrorMessage(body: String?): String? {
    if (body.isNullOrBlank()) return null
    return try {
        gson.fromJson(body, FastApiErrorDetail::class.java)?.detail
            ?: gson.fromJson(body, WrappedErrorMessage::class.java)?.message
    } catch (_: Exception) {
        null
    }
}
