package com.meya.doumenculture.data.network

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.meya.doumenculture.data.model.Venue
import java.lang.reflect.Type

class VenueDeserializer : JsonDeserializer<Venue> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Venue {
        val obj = json.asJsonObject

        val id = obj.get("id")?.asInt ?: 0
        val name = obj.get("name")?.asString ?: ""
        val imageURL = obj.get("image_url")?.takeIf { !it.isJsonNull }?.asString
        val description = obj.get("description")?.takeIf { !it.isJsonNull }?.asString ?: ""
        val capacity = obj.get("capacity")?.takeIf { !it.isJsonNull }?.asString ?: ""
        val area = obj.get("area")?.takeIf { !it.isJsonNull }?.asString ?: ""
        val address = obj.get("address")?.takeIf { !it.isJsonNull }?.asString ?: ""
        val isOpen = obj.get("is_active")?.takeIf { !it.isJsonNull }?.asBoolean ?: false

        val facilities = obj.get("facilities")?.let { parseFacilities(it) } ?: emptyList()

        return Venue(
            id = id,
            name = name,
            imageURL = imageURL,
            description = description,
            capacity = capacity,
            area = area,
            facilities = facilities,
            address = address,
            isOpen = isOpen
        )
    }

    private fun parseFacilities(element: JsonElement): List<String> {
        return try {
            when {
                element.isJsonArray -> element.asJsonArray.map { it.asString }
                element.isJsonPrimitive && element.asJsonPrimitive.isString -> {
                    val raw = element.asString
                    com.google.gson.JsonParser.parseString(raw).asJsonArray.map { it.asString }
                }
                else -> emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
