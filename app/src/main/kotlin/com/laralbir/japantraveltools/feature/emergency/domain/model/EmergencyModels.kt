package com.laralbir.japantraveltools.feature.emergency.domain.model

data class EmergencyContactNumber(
    val id: String,
    val nameEn: String,
    val nameEs: String,
    val japaneseName: String,
    val phoneNumber: String,
    val descriptionEn: String,
    val descriptionEs: String,
    val iconName: String,
    val isPrimaryEmergency: Boolean = false
)

data class ConsularMissionItem(
    val id: String,
    val countryCode: String,
    val countryName: String,
    val city: String,
    val addressJapanese: String,
    val addressRomaji: String,
    val emergencyPhone24h: String,
    val standardPhone: String,
    val latitude: Double,
    val longitude: Double,
    val websiteUrl: String
)

data class MedicalEmergencyProfile(
    val fullName: String = "",
    val bloodType: String = "",
    val allergies: List<String> = emptyList(),
    val conditions: String = "",
    val medications: String = "",
    val insuranceCompany: String = "",
    val insurancePolicyNumber: String = "",
    val insuranceEmergencyPhone: String = "",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val emergencyContactRelation: String = ""
)

enum class EmergencyCategory {
    MEDICAL,
    POLICE,
    DISASTER,
    ALLERGY
}

data class EmergencyPhraseItem(
    val id: String,
    val category: EmergencyCategory,
    val spanishText: String,
    val englishText: String,
    val japaneseKanji: String,
    val romaji: String
)
